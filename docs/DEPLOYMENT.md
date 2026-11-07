# Deployment Guide — Inventory Management System

---

## Prerequisites

- Java 21+
- Node.js 18+
- MySQL 8.0+
- (Optional) Docker & Docker Compose

---

## Local Development

### 1. Database Setup

```bash
mysql -u root -p
```

```sql
CREATE DATABASE inventory_db;
```

Run the schema:

```bash
mysql -u root -p inventory_db < inventory_db.sql
```

### 2. Backend

```bash
cd Backend
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties with your DB credentials and JWT secret
./mvnw spring-boot:run
```

Backend runs at `http://localhost:5050`.

### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm start
```

Frontend runs at `http://localhost:3000`.

---

## Docker Compose Deployment (Recommended)

From the project root:

```bash
docker-compose up --build
```

This spins up:

- **MySQL** on port 3306
- **Backend** on port 5050
- **Frontend** on port 3000

### Environment Variables (Docker)

When using `docker-compose`, environment variables are read from the `.env` file at the project root. Copy `.env.example` to `.env` and configure:

| Variable              | Description                        | Default (Docker)            |
| --------------------- | ---------------------------------- | --------------------------- |
| `MYSQL_ROOT_PASSWORD` | MySQL root password                | `rootpassword`              |
| `MYSQL_DATABASE`      | Database name                      | `inventory_db`              |
| `MYSQL_USER`          | MySQL application user             | `root`                      |
| `MYSQL_PASSWORD`      | Application user password          | `rootpassword`              |
| `DB_HOST`             | MySQL host (use `mysql` in Docker) | `localhost`                 |
| `DB_PORT`             | MySQL port                         | `3306`                      |
| `JWT_SECRET`          | JWT signing key (min 32 chars)     | auto-generated              |
| `VITE_API_URL`        | Backend base URL for frontend      | `http://localhost:5050/api` |

---

## Advanced Features

### 1. File Upload (AWS S3)

Products support image upload via multipart form-data. The backend stores files in AWS S3 and returns a public URL.

**Backend config (`application.properties`):**

```properties
aws.accessKeyId=your-access-key
aws.secretKey=your-secret-key
aws.region=ap-southeast-1
aws.bucketName=your-bucket-name
```

**Frontend:** Send `imageFile` as a `File` object in the FormData when calling `useCreateProduct()` or `useUpdateProduct()`.

---

### 2. Dashboard with Charts

`GET /api/transactions/by-month-year?month={m}&year={y}` returns transactions for a specific month, grouped by day, suitable for bar/line chart rendering.

The frontend `DashboardPage` uses `useTransactionsByMonth(month, year)` to fetch monthly data and renders it via a chart library.

---

### 3. CSV Export

`GET /api/transactions/export?filter={type}` triggers a server-side CSV generation capped at 10,000 rows to prevent OOM. The response is `text/csv` with `Content-Disposition: attachment`.

**Frontend:** Call the endpoint directly (e.g., via `window.open` or a hidden `<a>` tag), or use `api.get('/transactions/export', { params: { filter } })` to get the CSV string. The TransactionsPage should expose an "Export CSV" button accessible to all authenticated users.

---

## Live Deployment

| Service | URL |
|---------|-----|
| Frontend (Vercel) | https://inventory-management-sigma-puce.vercel.app |
| Backend API (Railway) | https://inventory-management-production-6424.up.railway.app/api |
| Swagger UI | https://inventory-management-production-6424.up.railway.app/swagger-ui/index.html |

---

## Production Deployment

### Backend (Railway / Render)

1. Connect your GitHub repository.
2. Set build command: `cd Backend && ./mvnw clean package -DskipTests`
3. Set start command: `java -jar Backend/target/backend-0.0.1-SNAPSHOT.jar`
4. Add environment variables:
   - `spring.datasource.url` — MySQL connection string
   - `spring.datasource.username` — DB username
   - `spring.datasource.password` — DB password
   - `secreteJwtString` — Strong random string (min 32 chars)

### Frontend (Vercel / Netlify)

1. Set root directory: `frontend`
2. Build command: `npm install && npm run build`
3. Output directory: `dist`
4. Environment variable: `VITE_API_URL` → your backend URL (e.g., `https://your-backend.railway.app/api`)

### MySQL (Railway / PlanetScale / MySQL Cloud)

1. Create a MySQL 8.0+ database.
2. Run `inventory_db.sql` to initialize the schema and seed data.
3. Whitelist the backend server's IP (or use a cloud-hosted DB with no IP restriction).
4. Set the connection string as `spring.datasource.url`.

---

## Environment Variables Reference

### Backend (`application.properties`)

| Variable                     | Description     | Example                                    |
| ---------------------------- | --------------- | ------------------------------------------ |
| `spring.datasource.url`      | MySQL JDBC URL  | `jdbc:mysql://localhost:3306/inventory_db` |
| `spring.datasource.username` | DB username     | `root`                                     |
| `spring.datasource.password` | DB password     | `yourpassword`                             |
| `secreteJwtString`           | JWT signing key | `YourSuperSecretKeyAtLeast32Characters!`   |
| `server.port`                | Backend port    | `5050`                                     |

### Frontend (`.env`)

| Variable       | Description      | Example                     |
| -------------- | ---------------- | --------------------------- |
| `VITE_API_URL` | Backend base URL | `http://localhost:5050/api` |
