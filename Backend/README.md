# Backend — Inventory Management System

Spring Boot 3.3.5 REST API for the Inventory Management System. Provides endpoints for products, categories, suppliers, warehouses, transactions, proposals, notifications, audit logs, users, and a dashboard.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | Bundled via Maven Wrapper (`mvnw.cmd`) |
| MySQL | 8.0+ |

---

## Configuration

Copy the example properties file and fill in your local values:

```bash
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

Key settings in `application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/inventory_db` |
| `spring.datasource.username` | DB username | `root` |
| `spring.datasource.password` | DB password | _(set locally)_ |
| `secreteJwtString` | JWT signing secret (min 32 chars) | _(required)_ |
| `server.port` | HTTP port | `5050` |
| `app.upload.directory` | Directory for uploaded product images | `uploads` |
| `cors.allowed-origins` | Comma-separated allowed CORS origins | `http://localhost:3000` |
| `spring.servlet.multipart.max-file-size` | Max upload file size | `10MB` |

All properties support environment variable overrides — see the root `README.md` for the full reference table.

---

## Running Locally

From the `Backend/` directory:

```bash
.\mvnw.cmd spring-boot:run
```

The server starts at `http://localhost:5050`.

Swagger UI is available at:

```
http://localhost:5050/swagger-ui/index.html
```

---

## Running Tests

### Prerequisites

Tests use a **separate database** (`inventory_db_test`) to avoid corrupting development data. Create it before running tests:

```sql
CREATE DATABASE inventory_db_test;
USE inventory_db_test;
SOURCE ../database/inventory_db_schema.sql;
```

> The test suite seeds and cleans up its own data via `@BeforeEach` / `@AfterEach`. Do **not** load `inventory_db_data.sql` into the test database.

### Test configuration

Tests use the `test` Spring profile (`@ActiveProfiles("test")`), which reads from `src/test/resources/application-test.properties`. Override the defaults with environment variables if your test database differs:

| Variable | Default |
|----------|---------|
| `TEST_DB_URL` | `jdbc:mysql://localhost:3306/inventory_db_test` |
| `TEST_DB_USERNAME` | `root` |
| `TEST_DB_PASSWORD` | _(set in properties file)_ |

### Run all tests

```bash
.\mvnw.cmd test
```

### Run a specific test class

```bash
.\mvnw.cmd test -Dtest=AuthControllerTest
```

### Test coverage overview

| Test class | Type | What it covers |
|------------|------|----------------|
| `BackendApplicationTests` | Context load | Spring context starts without errors |
| `AuthControllerTest` | Integration | `POST /api/auth/login` — valid credentials, wrong password, non-existent email |
| `ProductControllerTest` | Integration | Product CRUD via MockMvc with JWT |
| `ProductServiceTest` | Unit | `saveProduct`, `updateProduct`, `deleteProduct`, `getById` |
| `CategoryControllerTest` | Integration | Category CRUD, 401/403 boundary checks |
| `SupplierControllerTest` | Integration | Supplier CRUD, status toggle |
| `UserControllerTest` | Integration | User management — create, update, delete (ADMIN only) |
| `TransactionControllerTest` | Integration | Purchase, sell, return, status update |
| `TransactionServiceTest` | Unit | Stock deduction, oversell prevention, proposal auto-creation |
| `SecurityBoundaryTest` | Integration | 401 with no token, 403 with insufficient role |

> **Note on transactions:** `@Transactional` / `@Rollback` are declared on test classes but have **no effect** on cleanup, because repositories use raw JDBC connections outside Spring's transaction manager. Cleanup is performed explicitly in `@AfterEach` via direct JDBC deletes. See `TestUtils.java` for shared helpers.

---

## Building a Production JAR

```bash
.\mvnw.cmd clean package -DskipTests
```

The output JAR is at `target/backend-0.0.1-SNAPSHOT.jar`. Run it with:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Pass environment variables to override configuration in production:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://host:3306/inventory_db \
  --spring.datasource.password=secret \
  --secreteJwtString=YourProductionSecret
```

---

## Project Structure

```
src/
├── main/
│   ├── java/org/pdm/backend/
│   │   ├── controller/        REST controllers (thin — delegate to service)
│   │   ├── service/           Business logic interfaces
│   │   ├── service/Impl/      Service implementations
│   │   ├── repository/        Repository interfaces
│   │   ├── repository/Impl/   Raw JDBC implementations (JdbcTemplate + PreparedStatement)
│   │   ├── model/             Plain Java model classes (no @Entity)
│   │   ├── wrappers/          Request DTOs and the shared Response envelope
│   │   ├── enums/             UserRole, TransactionType, TransactionStatus, etc.
│   │   ├── security/          SecurityConfig, AuthFilter (JWT), JwtUtils, CorsConfig
│   │   ├── exception/         GlobalExceptionHandler and custom exception classes
│   │   ├── config/            OpenAPI / Swagger config, upload resource serving
│   │   └── util/              CsvExportUtils
│   └── resources/
│       ├── application.properties         Active configuration
│       └── application.properties.example Template for local setup
└── test/
    └── java/org/pdm/backend/
        ├── controller/        Integration tests (MockMvc + @SpringBootTest)
        ├── service/           Unit tests
        └── util/TestUtils.java  Shared test helpers (insert/delete test rows)
```

---

## Architecture Notes

- **No JPA.** All repositories use raw JDBC. `JdbcTemplate` is preferred for new repositories; `SupplierRepositoryImpl` and `ProductRepositoryImpl` use raw `Connection`/`PreparedStatement` (legacy).
- **Single `Response` envelope.** All endpoints return `Response` with `@JsonInclude(NON_NULL)`. Null fields are stripped from the JSON output automatically.
- **JWT only — no sessions.** The `AuthFilter` extracts the Bearer token from every request. No cookies or HTTP sessions are used. CSRF protection is disabled (appropriate for stateless JWT APIs).
- **Products use `multipart/form-data`**, not JSON, because image upload is part of create/update.

See `../docs/DECISIONS.md` for full reasoning behind non-obvious design choices.
