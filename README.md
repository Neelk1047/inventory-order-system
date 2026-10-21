# Inventory & Order Management System — REST API

A backend REST API for managing product inventory and customer orders, built with Java, Spring Boot, Spring Data JPA, and MySQL. Placing an order validates and reduces product stock in a single transaction, and freezes the price at time of purchase.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA (Hibernate)
- MySQL
- Maven
- Bean Validation (`@Valid`)
- Swagger / OpenAPI (springdoc-openapi) — interactive API documentation
- JUnit 5 + Mockito (unit testing)

## Architecture

Layered architecture separates concerns:

```
Controller  -->  Service  -->  Repository  -->  Database
(HTTP layer)   (business logic)  (data access)
```

- **Entities** — `Product`, `Order`, `OrderItem` (3 relational tables; `Order` has a one-to-many relationship with `OrderItem`)
- **Request DTOs** — validate and shape incoming data (`ProductRequestDTO`, `ProductBatchRequestDTO`, `OrderRequestDTO`, `OrderItemRequestDTO`)
- **Response DTOs** — shape outgoing data and avoid exposing entities or JPA internals directly (`ProductResponseDTO`, `OrderResponseDTO`, `OrderItemResponseDTO`)
- **Custom exceptions** — `ProductNotFoundException`, `InsufficientStockException`, `OrderNotFoundException`
- **Global exception handling** — `@RestControllerAdvice` converts all errors into consistent JSON responses

## Key design decisions

- **`@Transactional` order placement** — stock is checked and deducted for every item in one transaction. If any item fails (e.g. insufficient stock), the whole order rolls back and no partial stock deductions remain.
- **Unit price snapshot** — `OrderItem.unitPrice` captures the product's price at the moment of purchase, so a later price change never rewrites the cost of a past order.
- **`BigDecimal` for all money fields**, not `double` — avoids binary floating-point rounding errors in financial calculations.
- **Search + pagination + sorting** on `GET /products` — supports large catalogs without returning everything at once.
- **Request validation** — `@Valid` with constraints such as `@NotBlank`, `@Positive`, `@Min` and `@NotEmpty`; validation failures return field-level error messages.
- **DTOs instead of entities** — API contracts stay independent of the database model.

## Endpoints

| Method | Endpoint                            | Description                               |
| ------ | ----------------------------------- | ----------------------------------------- |
| POST   | `/products`                         | Create a new product                      |
| POST   | `/products/batch`                   | Create multiple products in one request   |
| GET    | `/products?name=&page=&size=&sort=` | List/search products, paginated           |
| GET    | `/products/{id}`                    | Get a single product                      |
| PUT    | `/products/{id}`                    | Update a product                          |
| DELETE | `/products/{id}`                    | Delete a product                          |
| POST   | `/orders`                           | Place an order (reduces stock atomically) |
| GET    | `/orders`                           | List all orders                           |
| GET    | `/orders/{id}`                      | Get a single order                        |

## Error responses

| Situation                          | HTTP status |
| ---------------------------------- | ----------- |
| Product or order not found         | 404         |
| Insufficient stock                 | 400         |
| Request validation failure         | 400         |
| Unexpected server error            | 500         |

Example:

```json
{ "error": "Insufficient stock for product 'Laptop'. Available: 2, Requested: 5" }
```

## Setup

1. Create the MySQL database:

```sql
CREATE DATABASE inventory_db;
```

2. Update `src/main/resources/application.properties` with your own MySQL username and password (do not commit real credentials).
3. Run the app:

```
mvn spring-boot:run
```

4. The API runs on `http://localhost:8080`.
5. View interactive API documentation (Swagger UI) at: `http://localhost:8080/swagger-ui/index.html`

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

## Testing

Run unit tests with:

```
mvn test
```

7 unit tests using JUnit 5 and Mockito, so no real database is needed:

- `ProductServiceTest` (5) — get by ID, product not found, create, update, delete
- `OrderServiceTest` (2) — stock reduction with correct total calculation, rejection when stock is insufficient

## Future improvements

- Optimistic locking (`@Version`) or a pessimistic lock on products to prevent overselling when orders are placed concurrently
- Integration tests (e.g. Testcontainers) to verify transaction rollback against a real database
- Authentication and authorization (e.g. Spring Security with JWT)
