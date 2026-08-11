# Mystore API

REST API de loja com Spring Boot 4.0.7, Java 21, Gradle e PostgreSQL (Flyway para migrações).

## Recursos

| Recurso | Endpoint | Descrição |
|---|---|---|
| Customers | `/customers` | CRUD de clientes |
| Payments | `/payments` | Pagamentos (aumentam o `balance` do cliente) |
| Products | `/products` | CRUD de produtos (`stock` >= 0, nome único) |
| Orders | `/orders` | Pedidos (listagem com ou sem filtro por `customerId`) |
| Order Items | `/orders/{orderId}/items` | Itens do pedido (subtraem o `balance` e decrementam o `stock`) |

## Regras de negócio

- `Payment` soma `value` ao `balance` do `Customer` (create), ajusta pela diferença (update) e subtrai (delete).
- `OrderItem` subtrai `price` do `balance` e decrementa `product.stock` em 1 (create exige `stock >= 1`); update de `price` ajusta o `balance` pela diferença; delete reembolsa `price` e incrementa `stock` em 1.
- Deletar um `Order` reembolsa o `balance` e devolve o `stock` de todos os itens (cascade no banco).
- Nomes de `Customer` e `Product` são únicos (case insensitive).
- Concorrência protegida por optimistic locking (`@Version`) em `Customer` e `Product` — conflito retorna `409`.

## Pré-requisitos

- Docker (para o PostgreSQL)
- JDK 21
- `.env` com `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (veja `.env.example`)

## Como rodar

```bash
# 1. Suba o banco
docker compose -f docker-compose.yaml up -d

# 2. (testes) exporte as variáveis do .env — o spring-dotenv não carrega .env nos workers de teste
export DB_URL=$(grep DB_URL .env | cut -d= -f2)
export DB_USERNAME=$(grep DB_USERNAME .env | cut -d= -f2)
export DB_PASSWORD=$(grep DB_PASSWORD .env | cut -d= -f2)

# 3. Execute
./gradlew bootRun    # inicia a aplicação (http://localhost:8080)
./gradlew test       # roda os testes
./gradlew build      # build completo (inclui bootJar)
```

Swagger UI (springdoc-openapi): `http://localhost:8080/swagger-ui.html`

## Migrações

Schema gerenciado por Flyway em `src/main/resources/db/migration/` (`V1__create_initial_tables.sql`, `V2__add_version_to_customers.sql`, `V3__add_version_to_products.sql`). `spring.jpa.hibernate.ddl-auto=validate` — toda mudança de entidade exige nova migração.
