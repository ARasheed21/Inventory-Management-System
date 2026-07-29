# Inventory Management System

This project is an enterprise modular monolith for inventory and order management.

## Phase 0 completed
- Spring Boot Maven project initialized
- Java 21 configured
- PostgreSQL local development setup
- OpenAPI/Swagger support enabled
- health endpoint and smoke test added

## Run locally
1. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```
3. Open health endpoint:
   ```text
   http://localhost:8080/health
   ```
