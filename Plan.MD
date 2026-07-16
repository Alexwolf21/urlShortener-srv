# Development Plan & PR Tracking Log

This log tracks our incremental design decisions, roadmap phases, and PR status for the URL Shortener & Link Analytics application.

---

## The Incremental Roadmap

| PR #  | Scope & Focus | Status | Details / Key Decisions |
| :--- | :--- | :---: | :--- |
| **PR-1** | **Documentation & Repository Blueprint** | **In Progress** | Establish `Gravity.MD`, `README.md`, `Plan.MD`. Define architecture blueprints and package organization. |
| **PR-2** | **Core API & Persistence (Walking Skeleton)** | Planned | Implement basic shortening REST APIs, validation, JPA entities, and database storage (H2). Implement custom alias validation and handle duplicate URLs via a pure append method. |
| **PR-3** | **Collision-Free Sequence-Based Generator** | Planned | Implement the Base62 encoder/decoder, mathematical permutation layer (obfuscation), and sequence allocator. Verify uniqueness through concurrent tests. |
| **PR-4** | **Caching Layer & Production Configurations** | Planned | Add Redis integration for sub-millisecond redirection read speeds. Set up Spring profile-specific configs (local H2 vs prod PostgreSQL). Add OpenAPI specifications. |
| **PR-5** | **Link Analytics, Rate Limiting & Write-Up** | Planned | Log click metrics asynchronously to keep redirect times fast. Implement rate limiting on writes (`POST /shorten`). Complete the take-home assessment write-up. |

---

## Design Log & Decision Registry

### Decision 1: Pure Append Strategy for Write Path (PR-1 Planning)
- **Problem**: Pre-checking `long_url` existence (`SELECT short_code FROM url_mappings WHERE long_url = ?`) introduces an extra DB read latency.
- **Alternatives Considered**:
  - *Option A*: De-duplicate (Read-before-Write). Return the same short code.
  - *Option B*: Pure Append (Write-only). Allow duplicates, generate new short code every time.
- **Decision**: **Option B (Pure Append)**.
- **Rationale**:
  1. High performance: Eliminates read latency, turning `POST /shorten` into a single INSERT.
  2. Business-accurate: Marketing teams need distinct short codes for the same destination to track campaign efficacy across different networks (e.g. email vs. social media).

### Decision 2: Short Code Generator Uniqueness (PR-1 Planning)
- **Problem**: How to generate short codes without colliding and without needing to check the database on every insert.
- **Alternatives Considered**:
  - *Option A*: MD5/SHA-256 Hash + Truncation. Has high collision risk (Birthday Paradox), requiring check-and-retry DB reads.
  - *Option B*: Random Character String. High collision risk as database size grows, causing write performance degradation over time.
  - *Option C*: Sequential Counter + Base62 Encoding. Guaranteed unique.
- **Decision**: **Option C (Counter + Base62)**.
- **Security Mitigation**: Sequential IDs are predictable. We will inject a mathematical bijective permutation function between the counter and the Base62 encoder to obscure the sequence without losing uniqueness.

---

## PR-1 Implementation Checklists
- [x] Configure `.gitignore` to exclude developer-facing `Gravity.MD`.
- [x] Create `Gravity.MD` (internal design details & codebase mapping).
- [x] Create `README.md` (public guide & quickstart instructions).
- [x] Create `Plan.MD` (tracking & architecture log).
- [x] Run test build verifying baseline Maven skeleton.

