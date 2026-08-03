# Inventory Management System

This project is a Spring Boot 3 application for managing inventory, products, and customer orders in a modular monolith architecture. It provides a basic but working end-to-end workflow for creating products, browsing the catalog, placing orders, processing payments, and tracking order lifecycle state.

## What the project does

- Manages a product catalog with admin create/update operations
- Supports customer-facing product browsing
- Creates and tracks orders through pending, paid, shipped, delivered, and cancelled states
- Exposes REST APIs documented with OpenAPI/Swagger
- Includes security with basic authentication for admin and customer roles
- Provides audit and reservation-related infrastructure for order lifecycle handling

## Technology stack

- Java 21
- Spring Boot 3.3.2
- Spring Data JPA + Hibernate
- PostgreSQL (development runtime)
- H2 (test profile)
- Spring Security
- Springdoc OpenAPI / Swagger UI
- Maven
- Docker Compose

## Prerequisites

- Java 21 or later
- Maven 3.9+
- Docker Desktop (recommended for running PostgreSQL locally)

> This project requires Java 21. If you see a Spring Boot plugin error such as `UnsupportedClassVersionError` or `class file version 61.0`, your installed Java is too old. Install JDK 21 and make sure `java -version` reports 21 before running Maven. On Windows, you may also need to restart your terminal after updating `JAVA_HOME` and `Path`.

> If Docker is not installed or not available on your PATH, the application can still be started in its test profile for local verification, but the default PostgreSQL-backed runtime will not be available until Docker is installed.

## Run the application locally

### Option 1: Use Docker (recommended)

1. Start the PostgreSQL container:
   ```bash
   docker compose up -d
   ```

2. Start the application:
   ```bash
   mvn spring-boot:run
   ```

### Option 2: Use the test profile if Docker is unavailable

If Docker is not installed, you can still run the application using the H2-backed test profile:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=test"
```

If Maven still fails before the app starts, verify your Java version first:

```powershell
java -version
mvn -version
```

The output should show Java 21 for both commands. If it does not, install or switch to JDK 21 and reopen the terminal.

### Open the application
   - Health endpoint: http://localhost:8080/health
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI JSON: http://localhost:8080/v3/api-docs

## Default credentials

The application includes simple in-memory security users for local development:

- Admin: `admin / admin`
- Customer: `customer / customer`

## Example API flows

### Create a product (admin)

```bash
curl -u admin:admin -X POST http://localhost:8080/api/inventory/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Desk Lamp",
    "description": "Compact office lamp",
    "price": "45.00",
    "currency": "USD",
    "quantityInStock": 25
  }'
```

### Browse products (customer or admin)

```bash
curl -u customer:customer http://localhost:8080/api/products
```

### Create an order

```bash
curl -u customer:customer -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-1",
    "items": [
      { "productId": "product-1", "quantity": 2 }
    ]
  }'
```

## Run the tests

Run the full test suite:

```bash
mvn test
```

Run a focused integration test:

```bash
mvn -Dtest=InventoryControllerIntegrationTest test
```

## Project structure

- `src/main/java` contains the application, domain, infrastructure, and web layers
- `src/test/java` contains unit and integration tests
- `docs/` contains product requirements and implementation planning documents

## Notes

- The default runtime profile uses PostgreSQL.
- The test profile uses H2, so the automated tests can run without a local database.
- The API documentation is generated automatically from the controller annotations.
