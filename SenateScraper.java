package com.example.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SenateScraper {

    // Simple JSON serialization
    public static class Senator {
        public String name;
        public String title;
        public String position; 
        public String party;
        public String address;
        public String phone;
        public String email;
        public String url;
        public Map<String,String> otherinfo = new HashMap<>();
    }

    public static void main(String[] args) throws Exception {
        String url = "https://akleg.gov/senate.php";

        // Setup ChromeDriver 
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        // run headless 
        opts.addArguments("--no-sandbox");
        opts.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(opts);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(8));

        try {
            driver.get(url);

            // following is the list of members
            // and what all members anchors inside it.
            List<WebElement> memberAnchors = driver.findElements(By.xpath(
                    "//h1[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'state senate')]" +
                    "/following::ul[1]//a[contains(@href,'www.akleg.gov')]"
            ));

            // In case structure differs, as fallback gather anchors having 'Email Me' parent text patterns.
            if (memberAnchors.isEmpty()) {
                memberAnchors = driver.findElements(By.xpath("//a[contains(@href,'www.akleg.gov')]"));
            }

            // to avoid duplicates if any
            LinkedHashSet<String> seenUrls = new LinkedHashSet<>();
            List<Senator> result = new ArrayList<>();

            for (WebElement anchor : memberAnchors) {
                // get the href and visible text
                String href = anchor.getAttribute("href");
                String name = anchor.getText().trim();
                if (name.isEmpty()) continue;
                if (href == null) href = "";

                // avoid duplicates by anchor 
                String uniqueKey = href.isEmpty() ? name : href;
                if (seenUrls.contains(uniqueKey)) continue;
                seenUrls.add(uniqueKey);

                // Typically the page shows details in the same block; we fetch parent's text.
                WebElement li = null;
                try {
                    li = anchor.findElement(By.xpath("./ancestor::li[1]"));
                } catch (Exception ignore) {}

                String blockText = "";
                if (li != null) {
                    blockText = li.getText();
                } else {
                    // fallback: grab a few nearby siblings
                    blockText = anchor.getText();
                    try {
                        WebElement parent = anchor.findElement(By.xpath("./.."));
                        blockText += "\n" + parent.getText();
                    } catch (Exception ignore) {}
                }

                // Create senator object
                Senator s = new Senator();
                s.name = name;
                s.url = href;

                // Parse Party, City/District, Phone, Toll-Free, Fax, Email from block text using regexes
                // Example patterns in page: "City: Anchorage", "Party: Republican", "Phone: 907-465-4919", "Toll-Free: 888-465-4919"
                s.party = findFirstMatch(blockText, "Party[:\\s]*([A-Za-z\\- ]+)");
                String city = findFirstMatch(blockText, "City[:\\s]*([^\\n\\r]+)");
                String district = findFirstMatch(blockText, "District[:\\s]*([^\\n\\r]+)");
                String phone = findFirstMatch(blockText, "Phone[:\\s]*([0-9\\-\\(\\) ]+)");
                String tollfree = findFirstMatch(blockText, "Toll[- ]?Free[:\\s]*([0-9\\-\\(\\) ]+)");
                String fax = findFirstMatch(blockText, "Fax[:\\s]*([0-9\\-\\(\\) ]+)");
                String emailHref = "";

                // Try to find mailto anchor inside li (Email Me links)
                try {
                    if (li != null) {
                        List<WebElement> mailLinks = li.findElements(By.xpath(".//a[starts-with(@href,'mailto:')]"));
                        if (!mailLinks.isEmpty()) {
                            emailHref = mailLinks.get(0).getAttribute("href");
                        } else {
                            // sometimes "Email Me" doesn't include mailto and is JS; try anchor text "Email Me"
                            List<WebElement> maybe = li.findElements(By.xpath(".//a[contains(normalize-space(.),'Email Me')]"));
                            if (!maybe.isEmpty()) {
                                String onclick = maybe.get(0).getAttribute("href");
                                if (onclick != null && onclick.startsWith("mailto:")) emailHref = onclick;
                            }
                        }
                    }
                } catch (Exception ignore) {}

                String email = "";
                if (emailHref != null && emailHref.startsWith("mailto:")) {
                    email = emailHref.substring("mailto:".length());
                }

                // Construct address/position string
                String address = "";
                if (city != null && !city.isEmpty()) {
                    address += city.trim();
                }
                if (district != null && !district.isEmpty()) {
                    if (!address.isEmpty()) address += " | ";
                    address += "District: " + district.trim();
                }

                s.position = (district != null && !district.isEmpty()) ? ("District " + district) : city;
                s.phone = (phone != null && !phone.isEmpty()) ? phone.trim() : (tollfree != null ? tollfree.trim() : "");
                s.email = email;
                s.address = address;
                s.title = ""; // page doesn't always contain a formal title besides name; leave blank or parse if needed

                // Save other simple fields
                if (fax != null && !fax.isEmpty()) s.otherinfo.put("fax", fax.trim());
                if (tollfree != null && !tollfree.isEmpty()) s.otherinfo.put("toll_free", tollfree.trim());

                result.add(s);
            }

            // write to JSON file
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            File out = new File("senate.json");
            mapper.writeValue(out, result);

            System.out.println("Done. Wrote " + result.size() + " entries to " + out.getAbsolutePath());

        } finally {
            driver.quit();
        }
    }

    // helper 
    private static String findFirstMatch(String text, String regex) {
        if (text == null) return "";
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }
}
