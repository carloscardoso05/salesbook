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
- **`.env`** is loaded by `spring-dotenv` (`me.paulschwarz:spring-dotenv-bom:5.1.0`) and supplies the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` variables used in `application.properties`.
- Test starters (`data-jpa-test`, `webmvc-test`, `validation-test`, `flyway-test`) are on the classpath but not yet used by any test.

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
- The `Customer` entity is the only one with constructor + validation logic (`Assert` guards in setters).

## Known technical debt (planned improvements)

- **`customer.balance` lost-update** — `PaymentService.createPayment`/`updatePayment`/`deletePayment` do a read-modify-write on the customer row with no `@Version` or `SELECT ... FOR UPDATE`, so concurrent payments for the same customer can overwrite each other. TODO: pessimistic/optimistic lock, or derive balance from the `payments` table instead of a denormalized counter. Related drift: `CustomerService.updateCustomer` lets clients set `balance` directly, so it can diverge from the real sum of payments.
- **Controllers document 404 but return 500** — `orElseThrow()` → `NoSuchElementException` → 500. TODO: global exception handler (`@RestControllerAdvice`) mapping to `404 NOT_FOUND`.
