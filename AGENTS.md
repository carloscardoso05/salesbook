# AGENTS.md

## Prerequisites

- **PostgreSQL must be running** before `bootRun`, `test`, or `build`. Start it with:
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
./gradlew build          # full build incl. bootJar (also needs Postgres for tests)
```

## Schema and migrations

- Schema is **managed by Flyway**, not Hibernate.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate only verifies entity-to-table mappings. It will **never** create or alter tables.
- **Every entity change (new field, new entity, relationship change) must be accompanied by a new Flyway migration** in `src/main/resources/db/migration/`.
- Case-insensitive unique name columns (`customers.name`, `products.name`) use a **`lower(name)` unique index** (e.g. `create unique index customers_name_lower_idx on customers (lower(name));`).

## Tech stack & conventions

- **Spring Boot 4.0.7, Java 21, Gradle** (wrapper included).
- **Lombok**: entities use `@Getter` only, with **manual setters guarded by `org.springframework.util.Assert`** (no Lombok `@Setter`). Delta mutations use domain mutators on the entity (`Customer.addToBalance(BigDecimal)`, `Product.addToStock(Integer)`) instead of `setX(getX().add()/.subtract())`. Controllers and services use `@RequiredArgsConstructor` for constructor injection. DTOs are Java `record` types.
- **Transactions**: every service method is `@Transactional` — reads use `@Transactional(readOnly = true)`, writes use `@Transactional`.
- **Exception handling** is centralized in the `shared/` sub-package: `ApiExceptionHandler` (`@RestControllerAdvice`) plus `ApiError` (error response body), `NotFoundException`, and `DuplicateException`. Services throw the custom exceptions (or `IllegalArgumentException`); the handler maps them to `404`/`409`/`400` and also covers Spring-level errors (`400` for validation, `409` for optimistic-lock and data-integrity violations, `500` fallback). Keep the handler updated when adding new exceptions.
- **Create endpoints return `201 CREATED` with a `Location` header** (built with `ServletUriComponentsBuilder` from the current request).
- **`.env`** is loaded by `spring-dotenv` (`me.paulschwarz:springboot4-dotenv:5.1.0`) and supplies the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` variables used in `application.properties`.

## Business rules

- **`BalanceAdjustment`** records manual balance edits: when `updateCustomer` changes `balance`, a `BalanceAdjustment` is created with `value = newBalance - oldBalance` (positive increases, negative decreases). Zero-difference updates create no adjustment. The entity is **immutable** (no setters, no update/delete service methods or endpoints — read-only API at `/adjustments`).
- **`Payment`** increases `customer.balance` by `value` on create; update adjusts by the value difference; delete subtracts `value`.
- **`OrderItem`** affects `customer.balance` (opposite of Payment — it is an expense):
  - create: `balance -= price` and `product.stock -= 1` (creation fails with `400` if `stock < 1` before deducting);
  - update price: `balance += (oldPrice - newPrice)`;
  - delete: `balance += price` and `product.stock += 1`.
- **Deleting an `Order`** refunds `balance` and restocks **all** its items (the DB cascade removes the items).
- **Optimistic locking**: `Customer` (`V2`) and `Product` (`V3`) have `@Version Instant version`. The Product version protects the stock check-then-decrement race (`OptimisticLockingFailureException` → `409`).
- `balance`, `stock`, and `price` mutations on `Customer`/`Product` persist via JPA dirty checking (no explicit `save`).

## Package structure

All features follow the **vertical slice** pattern — a dedicated sub-package with entity → repository → service → controller → `dto/` sub-package:

- **`customer/`** — reference vertical slice. Use it as the pattern for new features.
- **`payment/`** — full vertical slice.
- **`adjustment/`** — full vertical slice, read-only (`BalanceAdjustment`).
- **`product/`** — full vertical slice.
- **`order/`** — full vertical slice (nested resource: `OrderItemController` lives under `orderitem/`).
- **`orderitem/`** — full vertical slice, exposed at `/orders/{orderId}/items`.

## Entity relationships

```
Customer 1──* Order       (RESTRICT on delete)
Customer 1──* Payment     (RESTRICT on delete)
Customer 1──* BalanceAdjustment (RESTRICT on delete)
Order    1──* OrderItem   (CASCADE on delete)
Product  1──* OrderItem   (RESTRICT on delete)
```

- Monetary fields use `numeric(12, 2)` / `BigDecimal` with `@Column(precision = 12, scale = 2)`.
- All entities have a `protected` no-arg constructor (JPA) plus a public constructor with `Assert` guards in the setters.

## Tests

- Unit tests with **Mockito + AssertJ**, mirroring the existing suites: `*ServiceTest` (service layer, mocked repositories, strict stubs, ids via `ReflectionTestUtils`) and `*Test` (entity guards). Suites exist for Customer, Payment, Product, Order, OrderItem, and Adjustment.
- Test starters (`data-jpa-test`, `webmvc-test`, `validation-test`, `flyway-test`) are on the classpath but not yet used by any test. Planned (not yet implemented): integration tests with `@SpringBootTest` (full balance/stock flow) and `@WebMvcTest` controller slice tests using `@MockitoBean`.

## Known technical debt (planned improvements)

- **`customer.balance` drift** — `CustomerService.updateCustomer` lets clients set `balance` directly, so it can diverge from the real sum of payments minus order items. Manual edits are now auditable via `BalanceAdjustment` (created automatically on balance change, `value = newBalance - oldBalance`). The concurrent-modification side was addressed with optimistic locking (`@Version Instant version` on `Customer`, `V2`). The client-editable `balance` itself is still exposed on `UpdateCustomerRequest`.
- **`spring-dotenv` does not load `.env` in test workers** — `./gradlew test` fails with `'url' must start with "jdbc"` unless `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` are exported in the shell before running.
- **N+1 on lazy customer lookups in list endpoints** — `PaymentDto.of` and `BalanceAdjustmentDto.of` read `getCustomer().getId()`/`getName()` on a `LAZY` association while mapping each page row, so `GET /payments` and `GET /adjustments` fire one extra query per row (1 + N). Currently acceptable (small pages/tables) and consistent across slices, but if these reads become hot, fix with `@EntityGraph` or `JOIN FETCH` queries (`findAllByCustomerId`, etc.).
