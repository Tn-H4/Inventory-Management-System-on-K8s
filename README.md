# Inventory Management System

Full-stack inventory management application for the Web Application Development course at HCMIU. The system supports internal warehouse operations: product and stock management, suppliers, warehouses, purchase/sell/return transactions, approval proposals, notifications, audit logs, CSV exports, and dashboard analytics.

Accounts are internal-use only. Users sign in with accounts created by an administrator; public self-registration is not available.

## Group Members

- Le Thai Minh Tin (ITCSIU24086) - Full-stack developer, Project Lead
- Huynh Ngoc Thuy Tien (ITITIU24065) - Front-end developer

## Live Demo

| | URL |
|---|---|
| Frontend | https://inventory-management-sigma-puce.vercel.app |
| Swagger UI | https://inventory-management-production-6424.up.railway.app/swagger-ui/index.html |

### Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin1@gmail.com | 12345678 |
| Manager | manager1@gmail.com | 12345678 |
| Staff | staff1@gmail.com | 12345678 |

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.3.5
- Spring Security with JWT (`jjwt` 0.12.6)
- Raw JDBC repositories with MySQL
- Lombok
- Spring Validation
- Springdoc OpenAPI / Swagger UI
- Local multipart product image uploads served from `/uploads/**`
- Maven Wrapper

> Note: the project includes `spring-boot-starter-data-jpa` as a dependency, but the application code uses raw JDBC repositories, not JPA entities or Spring Data repositories.

### Frontend

- React 19.2.0 with Create React App (`react-scripts`)
- React Router 7.9.5
- React Query 5.90.8
- React Hook Form 7.66.0
- Zod 4.1.12
- Axios 1.13.2
- Tailwind CSS
- Recharts 3.4.1

### Database

- MySQL 8+
- Schema and seed data in `database/`:
  - `database/inventory_db.sql` — DDL (tables, indexes, constraints)
  - `database/inventory_db_data.sql` — seed data (10 users, 20 suppliers, 30 products, 100 transactions)
  - `database/ER-diagram.jpg` — entity-relationship diagram

## Features

### Dashboard
- Summary metrics for inventory health (total products, low stock) and financial standing (retail value, inventory cost)
- Dynamic data visualization charts (Net Daily Sales, Warehouse Utilization)
- CSV dashboard export capabilities for Managers and Admins
![Dashboard Summary](screenshots/Dashboard1.png)
![Net Daily Sales Chart](screenshots/Dashboard2.png)
![Warehouse Utilization Chart](screenshots/Dashboard3.png)
### Products
- Full CRUD operations featuring a soft-delete architecture
- Multipart image upload support (JPEG/PNG/WEBP, 5MB max)
- Auto-generated SKUs, reorder threshold alerts, and out-of-stock indicators
- Advanced search, multi-parameter filtering, pagination, and CSV export
- Complex supplier relationship management (unit costs, preferred vendors)
![Products](screenshots/Products.png)
![Product Details](screenshots/ProductDetail.png)

### Transactions
- Distinct Purchase (receive), Sell (deduct), and Return workflows
- Built-in overselling protection to prevent negative inventory balances
- Staff-initiated transactions automatically generate pending proposals for Manager review
- Comprehensive transaction history log with dynamic sorting, status filtering, and CSV export
![Sell](screenshots/Sell.png)
![Purchase](screenshots/Purchase.png)
![Transactions](screenshots/Transactions.png)
![Transaction Details](screenshots/TransactionDetail.png)

### Suppliers, Categories, and Warehouses
- Centralized master data management with active/inactive status toggling
- Rich metadata tracking (tax codes, warehouse capacities, site managers)
- Dynamic relational linking (e.g., filtering available suppliers based on the selected product)
- Universal CSV export capabilities across all master data tables
![Suppliers](screenshots/Suppliers.png)
![Categories](screenshots/Categories.png)
![Warehouses](screenshots/Warehouses.png)

### Users and Access Control
- Secure JWT authentication with BCrypt password hashing
- Strict Role-Based Access Control (RBAC): `ADMIN`, `MANAGER`, and `STAFF`
- Frontend route protection (Higher-Order Components) and backend method-level security
- Centralized Admin dashboard for user provisioning and role assignment
- Self-service profile updates and secure password management
![Login](screenshots/Login.png)
![Role-based Control](screenshots/Role.png)
![Users](screenshots/Users.png)

### Proposals, Notifications, and Audit Logs
- Streamlined approval/rejection workflow for staff-submitted inventory actions
- Role-aware system notifications (e.g., Managers receive approval alerts, Admins do not) with unread counts
- Deep-dive administrative audit logs capturing exact user actions, timestamps, and payload details
![Proposals](screenshots/Proposals.png)
![Notifications](screenshots/Notifications.png)
![Audit Logs](screenshots/AuditLogs.png)

## Getting Started

### Prerequisites

- Java 21
- Node.js 18+ and npm
- MySQL 8+

### Database Setup

Create the database, then run the schema file followed by the seed file:

```sql
CREATE DATABASE inventory_db;
USE inventory_db;
```

```bash
mysql -u root -p inventory_db < database/inventory_db.sql
mysql -u root -p inventory_db < database/inventory_db_data.sql
```

### Backend Setup

From `Backend/`:

```bash
.\mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:5050
```

Swagger UI is available at:

```text
http://localhost:5050/swagger-ui/index.html
```

### Frontend Setup

From `frontend/`:

```bash
npm install
npm start
```

The frontend runs on:

```text
http://localhost:3000
```

Set the frontend API base URL in `frontend/.env`:

```properties
REACT_APP_API_BASE_URL=http://localhost:5050/api
```

## Configuration

Backend configuration is in `Backend/src/main/resources/application.properties`. An example file is available at `Backend/src/main/resources/application.properties.example`.

Common local settings:

```properties
server.port=5050
spring.datasource.url=jdbc:mysql://localhost:3306/inventory_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
secreteJwtString=replace_with_a_strong_secret
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
app.upload.directory=uploads
cors.allowed-origins=http://localhost:3000
```

Environment variable overrides supported by the current configuration:

| Variable               | Purpose                         | Default                                    |
| ---------------------- | ------------------------------- | ------------------------------------------ |
| `DB_URL`               | MySQL JDBC URL                  | `jdbc:mysql://localhost:3306/inventory_db` |
| `DB_USERNAME`          | MySQL username                  | `root`                                     |
| `DB_PASSWORD`          | MySQL password                  | local developer value                      |
| `JWT_SECRET`           | JWT signing secret              | none                                       |
| `APP_UPLOAD_DIRECTORY` | Local upload directory          | `uploads`                                  |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:3000`                    |

## API Overview

Base URL:

```text
http://localhost:5050/api
```

Main endpoint groups:

| Area              | Endpoint Prefix      | Notes                                  |
| ----------------- | -------------------- | -------------------------------------- |
| Authentication    | `/auth/login`        | Public login only                      |
| Products          | `/products`          | Product CRUD, search, export           |
| Categories        | `/categories`        | Category CRUD, status toggle, export   |
| Suppliers         | `/suppliers`         | Supplier CRUD, status toggle, export   |
| Warehouses        | `/warehouses`        | Warehouse CRUD, status toggle, export  |
| Product suppliers | `/product-suppliers` | Supplier links and unit costs          |
| Transactions      | `/transactions`      | Purchase, sell, return, status, export |
| Dashboard         | `/dashboard`         | Stats and CSV export                   |
| Proposals         | `/proposals`         | Approval workflow                      |
| Notifications     | `/notifications`     | Current-user notifications             |
| Users             | `/users`             | Profile and admin user management      |
| Audit logs        | `/audit-logs`        | Admin audit browsing/export            |

All protected endpoints require:

```http
Authorization: Bearer <jwt>
```

Most endpoints return the shared `Response` envelope. `GET /api/users/current` returns the current user object directly.

Important API quirks:

- Product create/update uses `multipart/form-data`.
- Product update sends `productId` as a form field.
- Product codes are generated by the backend as `SKU000001` from product IDs and are not user-editable.
- Transaction status update sends a raw JSON string, for example `"COMPLETED"`.
- Categories use 1-indexed pagination.
- Transactions use 0-indexed backend pagination; the frontend converts page numbers.

## Project Structure

```text
inventory-management/
|-- Backend/
|   |-- src/main/java/org/pdm/backend/
|   |   |-- config/
|   |   |-- controller/
|   |   |-- enums/
|   |   |-- exception/
|   |   |-- model/
|   |   |-- repository/
|   |   |-- security/
|   |   |-- service/
|   |   |-- util/
|   |   `-- wrappers/
|   |-- src/main/resources/
|   |-- src/test/
|   |-- pom.xml
|   `-- mvnw.cmd
|-- frontend/
|   |-- public/
|   |-- src/
|   |   |-- components/
|   |   |-- features/
|   |   |-- lib/
|   |   |-- pages/
|   |   `-- App.js
|   |-- package.json
|   `-- tailwind.config.js
|-- docs/
|-- screenshots/
|-- inventory_db.sql
`-- README.md
```

## Development Commands

Backend commands, from `Backend/`:

```bash
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Frontend commands, from `frontend/`:

```bash
npm start
npm test
npm run lint
npm run format
npm run build
```

Run `npm run format` before committing frontend changes.

## Known Issues and Limitations

- **Product images in production:** Images are stored on the Railway backend filesystem. Because Railway has an ephemeral filesystem, uploaded images do not persist across deployments. AWS S3 is the correct long-term solution.
- **No self-registration:** All user accounts are created by an administrator. This is an intentional design decision for an internal warehouse management system.
- **No JWT refresh tokens:** When the access token expires, users must sign in again.
- **Pagination inconsistency:** Categories use 1-indexed pages; transactions use 0-indexed pages (the frontend corrects for this automatically).

## License

This project was developed for educational purposes as part of the Web Application Development course at HCMIU.
