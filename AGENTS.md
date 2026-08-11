# AGENTS.md

## Prerequisites

- **PostgreSQL must be running** before `bootRun` or `test`. Start it with:
  ```
  docker compose -f api/docker-compose.yaml up -d
  ```
- There is **no H2, no Testcontainers, and no embedded database**. Every test that touches the Spring context hits the real Postgres.

## Build & run

All Gradle commands run from the `api/` subdirectory:
```bash
cd api
./gradlew bootRun       # start the app (also needs Postgres)
./gradlew test           # run all tests (also needs Postgres)
```

## Schema and migrations

- Schema is **managed by Flyway**, not Hibernate.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate only verifies entity-to-table mappings. It will **never** create or alter tables.
- **Every entity change (new field, new entity, relationship change) must be accompanied by a new Flyway migration** in `src/main/resources/db/migration/`.
- Migrations use the `citext` extension for case-insensitive unique name columns (`customers.name`, `products.name`).

## Tech stack & conventions

- **Spring Boot 4.1.0, Java 21, Gradle** (wrapper included).
- **Lombok**: entities use `@Getter`/`@Setter`, controllers and services use `@RequiredArgsConstructor` for constructor injection. DTOs are Java `record` types.
- **Transactions**: every service method is `@Transactional` — reads use `@Transactional(readOnly = true)`, writes use `@Transactional`.
- **Exception handling** is centralized in the `shared/` sub-package: `ApiExceptionHandler` (`@RestControllerAdvice`) plus `ApiError` (error response body), `NotFoundException`, and `DuplicateException`. Services throw the custom exceptions (or `IllegalArgumentException`); the handler maps them to `404`/`409`/`400` and also covers Spring-level errors (`400` for validation, `409` for optimistic-lock and data-integrity violations, `500` fallback). Keep the handler updated when adding new exceptions.
- **Create endpoints return `201 CREATED` with a `Location` header** (built with `ServletUriComponentsBuilder` from the current request).
- **`.env`** is loaded by `spring-dotenv` (`me.paulschwarz:spring-dotenv-bom:5.1.0`) and supplies the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` variables used in `application.properties`.

## Package structure

- **`Customer`** is the reference vertical slice (`customer/` sub-package with entity, repository, service, controller, DTOs). Use it as the pattern for new features.
- **`Payment`** now also has a full vertical slice (`payment/` sub-package). **`Product`, `Order`, and `OrderItem`** are still entity-only stubs in the root `api` package — no repositories, services, or controllers exist yet.
- New domain features should follow the Customer/Payment pattern: a dedicated sub-package with entity → repository → service → controller → DTOs.

## Entity relationships

```
Customer 1──* Order       (RESTRICT on delete)
Customer 1──* Payment     (RESTRICT on delete)
Order    1──* OrderItem   (CASCADE on delete)
Product  1──* OrderItem   (RESTRICT on delete)
```

- Monetary fields use `numeric(12, 2)` / `BigDecimal` with `@Column(precision = 12, scale = 2)`.
- The `Customer` and `Payment` entities have constructor + validation logic (`Assert` guards in setters).

## Tests

- Unit tests with **Mockito + AssertJ**, mirroring the existing suites: `CustomerServiceTest`/`PaymentServiceTest` (service layer, mocked repositories, strict stubs) and `CustomerTest`/`PaymentTest` (entity guards).
- Test starters (`data-jpa-test`, `webmvc-test`, `validation-test`, `flyway-test`) are on the classpath but not yet used by any test.

## Known technical debt (planned improvements)

- **`customer.balance` drift** — `CustomerService.updateCustomer` lets clients set `balance` directly, so it can diverge from the real sum of payments. The concurrent-modification side of this was addressed with optimistic locking (`@Version Instant version` on `Customer`, column added in `V2__add_version_to_customers.sql`); `ApiExceptionHandler` maps `OptimisticLockingFailureException` to `409 CONFLICT`.
- **`spring-dotenv` does not load `.env` in test workers** — `./gradlew test` fails with `'url' must start with "jdbc"` unless `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` are exported in the shell before running.
