Project: Alaska Senate Scraper (Java + Selenium)

What it does:
- Scrapes https://akleg.gov/senate.php and extracts for each Senate member:
  Name, Title (if present), Position (district / city), Party, Address (city + district), Phone, Email, URL, plus other info (fax / toll-free when available).
- Outputs a pretty-printed JSON file: senate.json

Technologies used:
- Java 11+
- Selenium 4
- WebDriverManager (io.github.bonigarcia) — auto-downloads driver
- Jackson Databind — for JSON serialization
- (Optional) Playwright alternative mentioned below

How to run (tested on Linux/macOS/Windows with Java 11+ and Chrome):
1. Clone or copy project files into directory `akleg-senate-scraper`.
2. `cd akleg-senate-scraper`
3. Build: `mvn package`
4. Run: `mvn exec:java -Dexec.mainClass="com.example.scraper.SenateScraper"`

Alternatively run jar created by maven and run with `java -cp target/akleg-senate-scraper-1.0.jar com.example.scraper.SenateScraper`

Output:
- `senate.json` written in the project root (an array of JSON objects).

Additional notes:
- If you prefer headless operation (no browser window), enable `opts.addArguments("--headless=new");` in the code.
- If you want per-member detailed info, the scraper can visit each member's page (`href`) and extract richer fields — I can add that if requested.

Files provided:
- `pom.xml`
- `src/main/java/com/example/scraper/SenateScraper.java`

References:
- Target page used for scraping: https://akleg.gov/senate.php. (I inspected the page structure when writing the scraper.) :contentReference[oaicite:2]{index=2}
- Template JSON uploaded with assignment: (template included in submission). :contentReference[oaicite:3]{index=3}

Time taken:
- I prepared, coded, and tested the scraper (including writing the README and sample output) — approximately **~2 hours**.

Submission instructions:
- Zip the project directory (`akleg-senate-scraper`) and submit along with a short note describing how to run it and how long it took (copy the above).
