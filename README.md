# Inventory & Order Management System — REST API

A backend REST API for managing product inventory and customer orders, built with Java, Spring Boot, Spring Data JPA, and MySQL. Placing an order validates and reduces product stock in a single transaction, freezes the price at time of purchase, and is safe against overselling when several orders arrive at the same time.

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Data JPA (Hibernate)
- MySQL 8
- Maven
- Bean Validation (`@Valid`)
- Swagger / OpenAPI (springdoc-openapi) — interactive API documentation
- Docker + Docker Compose — one-command setup
- JUnit 5, Mockito, MockMvc, and H2 (in-memory, tests only)

## Architecture

Layered architecture separates concerns:

```
Controller  -->  Service  -->  Repository  -->  Database
(HTTP layer)   (business logic)  (data access)
```

- **Entities** — `Product`, `Order`, `OrderItem` (3 relational tables; `Order` has a one-to-many relationship with `OrderItem`)
- **Request DTOs** — validate and shape incoming data (`ProductRequestDTO`, `ProductBatchRequestDTO`, `OrderRequestDTO`, `OrderItemRequestDTO`)
- **Response DTOs** — shape outgoing data and avoid exposing entities or JPA internals directly (`ProductResponseDTO`, `OrderResponseDTO`, `OrderItemResponseDTO`)
- **Custom exceptions** — `ProductNotFoundException`, `OrderNotFoundException`, `InsufficientStockException`, `ProductInUseException`
- **Global exception handling** — `@RestControllerAdvice` converts all errors into consistent JSON responses and logs unexpected failures without leaking internals to the client

## Key design decisions

- **`@Transactional` order placement** — stock is checked and deducted for every item in one transaction. If any item fails (e.g. insufficient stock), the whole order rolls back and no partial stock deductions remain.
- **Optimistic locking to prevent overselling** — `Product` has a `@Version` field. If two orders try to take the last item at the same moment, only one update succeeds; the other is rejected with `409 Conflict` and can simply be retried. This is verified by a multi-threaded concurrency test.
- **Unit price snapshot** — `OrderItem.unitPrice` captures the product's price at the moment of purchase, so a later price change never rewrites the cost of a past order.
- **Product-in-use protection** — a product that appears in past orders cannot be deleted (`409 Conflict`), so order history is never broken.
- **`BigDecimal` for all money fields**, not `double` — avoids binary floating-point rounding errors in financial calculations.
- **Pagination everywhere it matters** — `GET /products` supports search, pagination and sorting; `GET /orders` is paginated (newest first by default).
- **N+1 query prevention** — `default_batch_fetch_size` is set and order items are loaded in batches instead of one query per order. Open-in-view is disabled so database sessions are not held open during JSON serialization.
- **Request validation** — `@Valid` with constraints such as `@NotBlank`, `@Positive`, `@Min`, `@NotNull` and `@NotEmpty`; validation failures return field-level error messages.
- **DTOs instead of entities** — API contracts stay independent of the database model.
- **Externalized configuration** — database settings come from environment variables, so no credentials live in the repository.

## Endpoints

| Method | Endpoint                            | Description                                          |
| ------ | ----------------------------------- | ---------------------------------------------------- |
| POST   | `/products`                         | Create a new product                                 |
| POST   | `/products/batch`                   | Create multiple products in one request              |
| GET    | `/products?name=&page=&size=&sort=` | List/search products, paginated                      |
| GET    | `/products/{id}`                    | Get a single product                                 |
| PUT    | `/products/{id}`                    | Update a product                                     |
| DELETE | `/products/{id}`                    | Delete a product (blocked if it is part of an order) |
| POST   | `/orders`                           | Place an order (reduces stock atomically)            |
| GET    | `/orders?page=&size=&sort=`         | List orders, paginated (newest first by default)     |
| GET    | `/orders/{id}`                      | Get a single order                                   |

## Error responses

| Situation                                                  | HTTP status |
| ---------------------------------------------------------- | ----------- |
| Product or order not found                                 | 404         |
| Insufficient stock                                         | 400         |
| Request validation failure                                 | 400         |
| Malformed JSON body or wrong parameter type (e.g. `/products/abc`) | 400 |
| Deleting a product that is part of existing orders         | 409         |
| Concurrent modification (another order changed the stock first) | 409    |
| Database constraint violation                              | 409         |
| Unexpected server error (details are logged, not returned) | 500         |

Most errors use a single `error` field:

```json
{ "error": "Insufficient stock for product 'Laptop'. Available: 2, Requested: 5" }
```

Validation errors return one entry per invalid field:

```json
{ "name": "Product name cannot be empty", "price": "Price must be greater than zero" }
```

## Setup

### Option 1 — Docker (recommended)

Starts MySQL and the API together, with no local Java or MySQL needed:

```
docker compose up --build
```

The database password defaults to `rootpass`. To use your own, set `DB_PASSWORD` first (for example in a local `.env` file, which is git-ignored):

```
DB_PASSWORD=mysecret docker compose up --build
```

### Option 2 — Run locally with Maven

1. Make sure MySQL is running and create the database:

```sql
CREATE DATABASE inventory_db;
```

2. Provide your database credentials via environment variables (nothing is hard-coded in the repo):

| Variable      | Default                                      | Purpose                              |
| ------------- | -------------------------------------------- | ------------------------------------ |
| `DB_URL`      | `jdbc:mysql://localhost:3306/inventory_db`   | JDBC connection URL                  |
| `DB_USERNAME` | `root`                                       | Database username                    |
| `DB_PASSWORD` | *(empty)*                                    | Database password                    |
| `SHOW_SQL`    | `false`                                      | Set to `true` to log SQL statements  |

   Alternatively, create `src/main/resources/application-local.properties` (git-ignored) with `spring.datasource.username` and `spring.datasource.password`, and run with the `local` profile.

3. Run the app:

```
mvn spring-boot:run
```

   or, with the local profile: `mvn spring-boot:run -Dspring-boot.run.profiles=local`

### After starting

- The API runs on `http://localhost:8080`.
- Interactive API documentation (Swagger UI): `http://localhost:8080/swagger-ui/index.html`

## Example requests

**POST /products**

```json
{
  "name": "Laptop",
  "price": 55000.00,
  "stockQuantity": 10
}
```

**POST /products/batch**

```json
{
  "products": [
    { "name": "Mouse", "price": 500.00, "stockQuantity": 50 },
    { "name": "Keyboard", "price": 1200.00, "stockQuantity": 30 }
  ]
}
```

**GET /products?name=lap&page=0&size=10&sort=price,desc** — returns a paginated, filtered, sorted list of products.

**POST /orders**

```json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

**GET /orders?page=0&size=10** — returns the most recent orders first.

## Testing

Run all tests with:

```
mvn test
```

No MySQL or Docker is needed: tests that touch the database use an in-memory H2 database (configured in `src/test/resources/application.properties`).

17 tests across three levels:

- **Service unit tests (JUnit 5 + Mockito)**
  - `ProductServiceTest` (6) — get by ID, product not found, create, update, delete, and refusing to delete a product that is part of existing orders
  - `OrderServiceTest` (2) — stock reduction with correct total calculation, rejection when stock is insufficient
- **Controller tests (MockMvc, `@WebMvcTest`)** — verify HTTP status codes, validation messages and error JSON
  - `ProductControllerTest` (5) — 201 on create, 400 on blank name, 400 on malformed JSON, 404 when missing, 409 when deleting a product used in orders
  - `OrderControllerTest` (3) — 400 on insufficient stock, 409 on concurrent stock change, paginated order list
- **Concurrency test (`@SpringBootTest`)**
  - `OrderConcurrencyTest` (1) — two threads buy the last item at the same time; exactly one order succeeds, the other is rejected, and stock ends at 0, never negative

## Future improvements

- Integration tests with Testcontainers to verify behaviour (transaction rollback, locking) against a real MySQL instance instead of H2
- Automatic retry of orders that fail due to an optimistic-lock conflict
- Database migrations (e.g. Flyway) in place of `ddl-auto=update`
- Authentication and authorization (e.g. Spring Security with JWT)
- CI pipeline (e.g. GitHub Actions) to run the test suite on every push
