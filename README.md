# URL Shortener & Link Analytics Service

A production-grade, high-performance URL Shortener and Link Analytics service built with Spring Boot 3.x and Java 21.

This project is structured specifically to show clean architecture, solid design patterns, and engineering trade-offs suitable for high-scale systems.

---

## Key Features

- **Collision-Free Code Generation**: Utilizes a counter-based bijective encoding system to guarantee short codes never collide, avoiding expensive pre-insertion database lookups.
- **Optimized Write Path**: Pure append strategy that eliminates read queries on shortening.
- **Custom Aliases**: Users can define custom short codes with optimistic concurrency handling via database constraints.
- **Asynchronous Click Analytics**: Logs redirection statistics (IP, user-agent, referrers) in a non-blocking asynchronous event loop to keep redirection latency low.
- **Robust Validation**: Rejects invalid, malformed, or unsafe URLs.
- **Clean Architecture**: Adheres strictly to Domain-Driven Design (DDD) layered patterns.

---

## Architectural Summary

### 1. Short Code Generation
Instead of using hash functions (e.g., MD5 or SHA-256) which suffer from collisions due to the Birthday Paradox, the application uses **Bijective Base62 Mapping of Obfuscated Sequential IDs**:
1. Retrieve a unique `ID` from a database sequence.
2. Apply a mathematical permutation (e.g., bit-mixing or multiplicative inverse) to obfuscate the ID to prevent predictability.
3. Encode the integer to a Base62 string using `[a-zA-Z0-9]`.

This generates short codes that look random (e.g., `a7X9p1`) but are mathematically guaranteed to be unique and collision-free.

### 2. High-Throughput Write Path (Shortening)
To shorten a URL, the system performs a pure append insert:
- No duplicate checks on the long URL are run, preventing read latency.
- No lookup checks on the generated short code are required since uniqueness is guaranteed.

### 3. High-Performance Read Path (Redirection)
- Redirections (`GET /{code}`) are designed to be read-through cached.
- Redis acts as the fast-lookup layer. If a key is present in Redis, the redirect is handled in sub-millisecond response times. If not, the mapping is fetched from PostgreSQL/H2 and written to cache.

---

## Tech Stack

- **Runtime**: Java 21
- **Framework**: Spring Boot 3.x
- **ORM / Persistence**: Spring Data JPA & Hibernate
- **Databases**:
  - Development/Testing: H2 Database (in-memory)
  - Production: PostgreSQL
- **Caching**: Spring Cache with Redis
- **Testing**: JUnit 5, Mockito, AssertJ

---

## Quickstart

### Prerequisites
- JDK 21
- Maven 3.x

### Build the Project
```bash
./mvnw clean package
```

### Run Tests
```bash
./mvnw clean test
```

### Run the Application Locally (Default Profile: H2 Database)
```bash
./mvnw spring-boot:run
```
The server will start at `http://localhost:8080`.
The H2 database console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:urlshortener`, Username: `sa`, Password: `sa`).

---

## API Endpoints

### 1. Shorten URL
Create a short code for a destination URL.

- **URL**: `POST /api/v1/shorten`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "longUrl": "https://www.paytm.com/recharge",
  "customAlias": "paytm-recharge"
}
```
- **Response (201 Created)**:
```json
{
  "shortCode": "paytm-recharge",
  "shortUrl": "http://localhost:8080/paytm-recharge",
  "longUrl": "https://www.paytm.com/recharge",
  "createdAt": "2026-07-17T03:30:00Z",
  "expiresAt": null
}
```

### 2. Redirect URL
Redirect to the original long URL.

- **URL**: `GET /{code}`
- **Response**: `301 Moved Permanently` (Location Header pointing to destination URL).
- **Error Response (404 Not Found)**: If the short code does not exist.
