# Engineering Write-Up: URL Shortener & Link Analytics

This document summarizes our design decisions, engineering trade-offs, and critical review points for the take-home URL Shortener evaluation.

---

### 1. What did you ask the AI to do, and what did you write or decide yourself?
* **What we asked the AI to do**:
  * Generate boilerplate Spring Boot configurations, record-type DTO wrappers, REST routing paths, and database JPA annotations.
  * Scaffold unit test files containing mock inputs (like URL validators and MockMvc calls).
  * Structure base conversion functions (such as decimal to Base62 loop).
* **What we decided ourselves (Architectural Direction)**:
  * **Counter-Based Bijective Generator**: We explicitly chose to reject random strings or hash truncation (due to collision risks) and designed a counter sequence generator.
  * **ID Obfuscation via Feistel Cipher**: We chose to implement a reversible 4-round Feistel Cipher over 62 bits to secure the sequence counter. This scrambles sequential counters into unpredictable values without losing uniqueness.
  * **Pure-Append Write Path**: We made the deliberate decision to skip duplicate checks on `POST /shorten`, avoiding index reads on the write path and naturally supporting distinct tracking for marketing campaigns.
  * **Optimistic Alias Checks**: Rather than checking database state before inserting custom aliases (prone to race conditions), we decided to write directly and catch the unique constraint exception.
  * **Decoupled Asynchronous Analytics**: We decided to process redirection logging (`GET /{code}`) out-of-band using Spring Application Events and `@Async` handlers to ensure redirections remain fast and non-blocking.
  * **IP Token Bucket Rate Limiting**: We chose to build a clean in-memory Token Bucket rate limiter to protect the shortening endpoint from abuse without adding heavy external dependencies.

---

### 2. Where did you override, correct, or throw away the AI’s output — and why?
* **Overriding Redirection Code (301 vs. 302)**: The AI initially suggested returning a `301 Moved Permanently` HTTP status code. We threw this out and returned a `302 Found` (Temporary Redirect) instead.
  * *Why*: Browsers cache 301 redirects locally. Subsequent clicks never hit our servers, making click-tracking analytics impossible. A 302 Found prevents browser caching and ensures we log 100% of clicks.
* **Rejecting Pre-Check DB Reads for Custom Aliases**: The AI code checked if a custom alias existed in Java before saving. We removed this pre-check.
  * *Why*: In high-concurrency environments, checking the DB and then inserting creates a race condition window where two concurrent threads could see the alias as free and both attempt to write it. We replaced it with optimistic constraint handling, relying solely on the database unique index constraint.
* **Cleaning Java sign-bit masking in Obfuscation**: The AI suggested using `Math.abs()` on the Feistel cipher's output, which would have discarded the sign bit and broken the bijective mapping (introducing potential collisions). We corrected this by constraining the Feistel cipher's math entirely to a 62-bit positive long range, preserving 100% collision-free uniqueness.

---

### 3. The two or three biggest trade-offs you made, and the alternatives you considered.
* **Trade-off 1: Duplicated Storage vs. Write Latency (Pure Append)**
  * *Alternative*: Query the database for the long URL first and return the existing short code.
  * *Trade-off*: By adopting a pure-append strategy, we allow multiple short codes to point to the same destination URL. This incurs a minor storage overhead in our database. However, this trade-off is highly beneficial: it eliminates DB read latency on shortening, and it is a business necessity for marketing teams tracking click metrics across different channels (e.g. social media vs. newsletter).
* **Trade-off 2: In-Memory Token Bucket vs. Redis Rate Limiting**
  * *Alternative*: Using Redis-backed token buckets or external libraries like Bucket4j.
  * *Trade-off*: An in-memory concurrent hashmap rate limiter limits requests per individual app instance rather than globally across a cluster. However, it keeps local development and unit tests completely independent of external infrastructure dependencies, reducing deployment complexity. For horizontal scaling, we can easily swap the `RateLimitingInterceptor` to fetch tokens from a central Redis server.
* **Trade-off 3: Sequence Allocation vs. Range Allocators**
  * *Alternative*: Grabbing sequence values directly from the database for every request.
  * *Trade-off*: Direct database sequence calls are simple and robust, but bottleneck the database under extreme concurrent load. We designed a range/segment counter strategy (allocating blocks of 1,000 IDs to service instances in-memory) to address this scale requirement.

---

### 4. What’s missing, or what you’d do with another day?
If we had another day to extend the system, we would implement:
1. **API Documentation**: Integrate Springdoc OpenAPI/Swagger to auto-generate beautiful API specifications.
2. **Distributed Redis Rate Limiter**: Transition our local in-memory Token Bucket map to Redis scripts to enforce global rate limits across multiple stateless service nodes.
3. **GeoIP Analytics Parsing**: Integrate a MaxMind GeoIP reader in our asynchronous click listener to parse client IP addresses into regions and countries, generating rich link analytics dashboards.
