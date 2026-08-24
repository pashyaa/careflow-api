# CareFlow API Delivery Backlog

This repository-specific view complements the master portfolio tracker kept in the local workspace. Estimates are intended for a developer with roughly three years of full-stack experience.

| ID | Type | Priority | Work item | Estimate |
|---|---|---|---|---:|
| CF-201 | Quality | High | PostgreSQL Testcontainers integration-test foundation | 8 h |
| CF-202 | Quality | High | Repository and REST-controller integration coverage | 8 h |
| CF-204 | Quality | Medium | OpenAPI contract and example payloads | 4 h |
| BUG-310 | Bug | Medium | Consistent Problem Details for malformed JSON, enums, and UUIDs | 3 h |
| CF-101 | Feature | Critical | JWT authentication and role-based authorization | 12 h |
| CF-112 | Feature | High | Derive audit identity from the authenticated principal | 5 h |
| CF-102 | Feature | Critical | Organization tenancy and query-level data isolation | 10 h |
| BUG-304 | Bug | High | Version preconditions and recoverable optimistic-lock conflicts | 6 h |
| CF-106 | Feature | Critical | Configurable site/category/priority SLA policies | 12 h |
| BUG-303 | Bug | High | Site-aware SLA time-zone handling | 6 h |
| BUG-305 | Bug | High | Required resolution and cancellation reason codes | 4 h |
| CF-103 | Feature | Medium | Service-site administration | 8 h |
| CF-104 | Feature | High | Asset lifecycle and service history | 10 h |
| BUG-306 | Bug | Medium | Block inactive/foreign sites from reference-data queries | 3 h |
| CF-105 | Feature | High | Technician skills, territory, availability, and capacity | 8 h |
| BUG-307 | Bug | High | Prevent or audit unsuitable technician assignments | 5 h |
| CF-107 | Feature | Medium | Reopen and SLA reschedule workflows | 5 h |
| CF-108 | Feature | High | Comments and secure attachment storage | 12 h |
| CF-109 | Feature | High | SLA and resolution analytics | 8 h |
| CF-111 | Feature | High | Transactional notification outbox and escalation events | 8 h |
| CF-207 | DevOps | Medium | Structured logging and request correlation | 5 h |
| CF-209 | Quality | High | Query profiling, N+1 removal, and index validation | 7 h |

## Backend definition of done

- Acceptance criteria and business rules have automated tests.
- `mvn clean verify` passes from a clean checkout.
- Schema changes use a new repeatable-from-empty Flyway migration.
- Authorization and tenant isolation are tested at repository and API boundaries.
- API failures use the documented Problem Details structure.
- README and `Docs/API_CONTRACT.md` reflect the delivered behavior.

