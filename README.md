# Inventory & Order Management System — REST API

A backend REST API for managing product inventory and customer orders, built with Java, Spring Boot, Spring Data JPA, and MySQL. Placing an order validates and reduces product stock in a single transaction, and freezes the price at time of purchase.

## Tech Stack
- Java 17
- Spring Boot
- Spring Data JPA (Hibernate)
- MySQL
- Maven
- Swagger / OpenAPI (springdoc-openapi) — interactive API documentation
- JUnit 5 + Mockito (unit testing)

## Architecture
Layered architecture separates concerns:

```
Controller  -->  Service  -->  Repository  -->  Database
(HTTP layer)   (business logic)  (data access)
```

- **Entity** — `Product`, `Order`, `OrderItem` (3 relational tables)
- **Request DTOs** — validate and shape incoming data (`ProductRequestDTO`, `OrderRequestDTO`)
- **Response DTOs** — shape outgoing data, avoid exposing entities or JPA internals directly (`ProductResponseDTO`, `OrderResponseDTO`, `OrderItemResponseDTO`)
- **Custom exceptions** — `ProductNotFoundException`, `InsufficientStockException`, `OrderNotFoundException`
- **Global exception handling** — `@RestControllerAdvice` centralizes all error responses into consistent JSON

## Key design decisions
- **BigDecimal for all money fields**, not `double` — avoids binary floating-point rounding errors in financial calculations.
- **Unit price snapshot** — `OrderItem.unitPrice` captures the product's price at the moment of purchase, so a later price change never rewrites the cost of a past order.
- **Search + pagination** on `GET /products` — supports large catalogs without returning everything at once.
- **`@Transactional` order placement** — if any item in a multi-item order fails (e.g. insufficient stock), the whole order rolls back; no partial stock deductions.

## Endpoints

| Method | Endpoint                          | Description                                  |
|--------|-------------------------------------|-----------------------------------------------|
| POST   | `/products`                         | Create a new product                          |
| GET    | `/products?name=&page=&size=&sort=` | List/search products, paginated               |
| GET    | `/products/{id}`                    | Get a single product                          |
| PUT    | `/products/{id}`                    | Update a product                              |
| DELETE | `/products/{id}`                    | Delete a product                              |
| POST   | `/orders`                           | Place an order (reduces stock atomically)     |
| GET    | `/orders`                           | List all orders                               |
| GET    | `/orders/{id}`                      | Get a single order                            |

## Setup

1. Create the MySQL database:
   ```sql
   CREATE DATABASE inventory_db;
   ```
2. Update `src/main/resources/application.properties` with your MySQL password.
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

**GET /products?name=lap&page=0&size=10&sort=price,desc**
Returns a paginated, filtered, sorted list of products.

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
`ProductServiceTest` and `OrderServiceTest` use Mockito to test business logic without needing a real database.
