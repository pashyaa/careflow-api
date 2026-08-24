# CareFlow API

REST API for CareFlow Service Operations, a field-service work-order and SLA tracking platform for facilities teams.

## Implemented starter scope

- Sites, assets, technicians, work orders, and immutable status-history records
- Work-order creation, search, filtering, paging, sorting, assignment, and controlled status transitions
- Dashboard counts for open, overdue, unassigned, and critical work
- Bean Validation, centralized Problem Details responses, Flyway migrations, optimistic locking, and health endpoints
- Realistic demonstration data for three customer sites
- JUnit 5, Mockito, and AssertJ unit tests for core workflow rules

## Technology baseline

- Java 21
- Spring Boot 4.1.1
- Maven 3.9+
- PostgreSQL 18.6
- Flyway, Spring Data JPA, Spring MVC, Validation, Actuator, JUnit 5

## Local setup with IntelliJ IDEA

1. Install JDK 21 and Docker Desktop. IntelliJ IDEA 2025.3 or newer is recommended.
2. Open this `careflow-api` folder as a Maven project.
3. Set **Project SDK** and the Maven runner JRE to Java 21.
4. Start the repository-local PostgreSQL service from this folder:

   ```powershell
   docker compose up -d
   ```

5. Run `CareFlowApplication` from IntelliJ, or use the Maven command below.

The default database is `careflow` on `localhost:5432`; Flyway creates and seeds it at startup.

## Commands

```powershell
# Run all unit tests
mvn test

# Compile and package
mvn clean verify

# Start the API
mvn spring-boot:run
```

API base URL: `http://localhost:8080/api/v1`

Health check: `http://localhost:8080/actuator/health`

## Useful requests

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/dashboard/summary

Invoke-RestMethod "http://localhost:8080/api/v1/work-orders?status=NEW&priority=CRITICAL"

Invoke-RestMethod http://localhost:8080/api/v1/reference/sites
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/careflow` | JDBC connection URL |
| `DB_USERNAME` | `careflow` | Database user |
| `DB_PASSWORD` | `careflow` | Database password |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | React development origin |
| `SERVER_PORT` | `8080` | HTTP port |

## Architecture

The code is organized by domain, repository, service, API/DTO, configuration, and exception-handling concerns. Controllers stay thin; transactions and workflow rules live in services; persistence entities are not returned directly from REST endpoints.

## Repository independence

This folder is a complete backend repository. It does not require files from the frontend repository or a shared parent directory. The frontend is an API client and can be hosted, versioned, and released separately.

See `Docs/API_CONTRACT.md` for endpoints and `Docs/BACKLOG.md` for the backend delivery roadmap.
