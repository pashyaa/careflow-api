# Backend Repository Guide

## Repository responsibility

`careflow-api` owns domain rules, persistence, REST contracts, database migrations, operational metrics, and backend tests. It must remain deployable without checking out the React repository.

## Integration boundary

- Default API URL: `http://localhost:8080/api/v1`
- Default allowed development origin: `http://localhost:5173`
- Consumers integrate through JSON REST endpoints and Problem Details errors.
- API changes should be backward compatible or released with an explicit migration note.

## Before the first remote push

1. Review `.gitignore`, `.env.example`, and the seed data for public sharing.
2. Run `mvn clean verify`.
3. Run `docker compose config --quiet`.
4. Start PostgreSQL and perform the README smoke requests.
5. Commit `pom.xml`, source, migrations, tests, documentation, and Compose configuration.
6. Never commit `.env`, IntelliJ metadata, `target`, credentials, or production customer data.

