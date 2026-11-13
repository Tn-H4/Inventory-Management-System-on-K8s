# Inventory Management System - Kubernetes Deployment Report

---

## I. Introduction

### 1.1 Overview
The **Inventory Management System (IMS)** is a full-stack, dynamic web application designed to handle enterprise warehouse operations. It provides features including product stock management, supplier coordination, purchase/sell/return transaction recording, multi-role approval proposals (Admin, Manager, Staff), automated email notifications, audit logs, and analytical dashboards.

This application is **fully dynamic**:
- Client-side views query a live REST API asynchronously.
- State, inventory counts, audit records, and auth tokens are persisted and evaluated dynamically against a transactional relational database (MySQL 8.0).
- Backend business logic, access control, and dynamic reports are processed in real-time by a Spring Boot application runtime.

The application is deployed on **Google Kubernetes Engine (GKE)** and accessible over the public internet.

### 1.2 Architecture
The system follows a multi-tier microservice-style containerized architecture deployed on Google Kubernetes Engine:

```
[ User Browser — Any Device, Anywhere ]
       │ http://136.85.39.249 (Port 80)
       ▼ (GCP LoadBalancer)
┌──────────────────────────────────────────────────────────────────┐
│  Google Kubernetes Engine (GKE) — asia-southeast1-a (Singapore)  │
│                                                                  │
│  ┌── Master Node (GKE-Managed Control Plane) ──────────────┐     │
│  │  kube-apiserver · etcd · kube-scheduler                 │     │
│  │  kube-controller-manager · cloud-controller-manager     │     │
│  └─────────────────────────────────────────────────────────┘     │
│                           │ Schedules pods                       │
│                           ▼                                      │
│  ┌── Worker Node (e2-standard-2, 2vCPU / 8GB RAM) ───────────┐   │
│  │                                                           │   │
│  │  ┌─────────────────────────┐                              │   │
│  │  │  Frontend Pod           │                              │   │
│  │  │  • Nginx Alpine Server  │                              │   │
│  │  │  • React 19 SPA Build   │                              │   │
│  │  │  • Reverse Proxy (/api) │                              │   │
│  │  └───────────┬─────────────┘                              │   │
│  │              │ http://backend:5050                        │   │
│  │              ▼                                            │   │
│  │  ┌─────────────────────────┐                              │   │
│  │  │  Backend Pod            │                              │   │
│  │  │  • Spring Boot 3.3.5    │                              │   │
│  │  │  • Java 21 LTS          │                              │   │
│  │  │  • JWT Auth & Security  │                              │   │
│  │  └───────────┬─────────────┘                              │   │
│  │              │ jdbc:mysql://mysql:3306                    │   │
│  │              ▼                                            │   │
│  │  ┌─────────────────────────┐   ┌────────────────────┐     │   │
│  │  │  Database Pod (mysql-0) │──▶│ PersistentVolume   │     │   │
│  │  │  • MySQL 8.0 Engine     │   │ (GCE Persistent    │     │   │
│  │  │  • StatefulSet          │   │  Disk, 1Gi RWO)    │     │   │
│  │  └─────────────────────────┘   └────────────────────┘     │   │
│  └───────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

#### Core Components:
1. **Frontend Tier (`ims-frontend`)**:
   - Built on React 19 with Tailwind CSS and React Router.
   - Hosted inside an Nginx Alpine container.
   - Serves static assets and acts as a reverse-proxy forwarding `/api/*` and `/uploads/*` requests to the backend service.
2. **Backend Tier (`ims-backend`)**:
   - RESTful API service built on Spring Boot 3.3.5 running on Java 21.
   - Handles JWT authentication, business validations, role authorizations, transactions, and CSV exports.
   - Uses an init-container (`busybox`) to wait for database network availability prior to starting.
3. **Database Tier (`mysql`)**:
   - Managed via a Kubernetes `StatefulSet` using the official `mysql:8.0` image.
   - Bound to a `PersistentVolumeClaim` (`mysql-pvc`) backed by a GCE Persistent Disk to ensure data durability across pod restarts.
   - Initialized via a Kubernetes `ConfigMap` (`mysql-init`) containing pre-configured schema tables and seed data.

### 1.3 Operation

#### Inter-Service Communication
- **User ➔ Frontend**: Users access the application via a public IP address (`http://136.85.39.249`) assigned by the GCP LoadBalancer. No port numbers or VPN required.
- **Frontend ➔ Backend**: When users perform actions, React dispatches Axios requests to `/api/...`. Nginx intercepts these paths and reverse-proxies them internally to `http://backend:5050/api/` via Kubernetes CoreDNS service discovery.
- **Backend ➔ Database**: The Spring Boot service connects to MySQL using JDBC over port `3306` (`jdbc:mysql://mysql:3306/inventory_db`). This traffic stays entirely within the cluster's internal network.

#### Docker Image Creation
Both Docker images use multi-stage builds to minimize image sizes and enhance security:
- **Backend (`Backend/Dockerfile`)**:
  - *Stage 1 (Build)*: Uses `eclipse-temurin:21-jdk-alpine`, pulls dependencies via Maven wrapper (`./mvnw`), and compiles code into a `.jar` package (`mvn package -DskipTests`).
  - *Stage 2 (Runtime)*: Uses lightweight `eclipse-temurin:21-jre-alpine` and copies only `app.jar` to run.
- **Frontend (`frontend/Dockerfile`)**:
  - *Stage 1 (Build)*: Uses `node:20-alpine`, installs NPM dependencies (`npm ci`), and creates an optimized static bundle (`npm run build`).
  - *Stage 2 (Runtime)*: Copies static production artifacts into `nginx:alpine` and mounts a custom reverse-proxy `nginx.conf`.

Images are stored in **Google Artifact Registry** (`asia-southeast1-docker.pkg.dev/ims-k8s-project/ims-repo/`), allowing GKE worker nodes to pull them directly from Google's internal network.

#### Master / Worker Node Communication
The deployment runs on **Google Kubernetes Engine (GKE)** with a managed control plane:
- **Control-Plane (Master Node)**: Fully managed by Google. Runs `kube-apiserver`, `etcd`, `kube-controller-manager`, `kube-scheduler`, and `cloud-controller-manager`. Google handles its availability, scaling, and security patching automatically.
- **Worker Node (`gke-ims-cluster-default-pool-*`)**: A GCE `e2-standard-2` VM (2 vCPU, 8 GB RAM) running Container-Optimized OS, `kubelet`, `containerd`, and `kube-proxy`.
- **How they interact**:
  1. `kubectl` or `gcloud` sends manifests to the `kube-apiserver` on the GKE-managed Master node.
  2. The `kube-scheduler` assigns pods (`mysql-0`, `backend`, `frontend`) to the Worker node.
  3. The Worker node's `kubelet` pulls images from Artifact Registry, talks to `containerd` to run the containers, and configures VPC-native networking.
  4. The GCP Cloud LoadBalancer forwards external internet traffic (port 80) to the `frontend` Service running on the Worker node.

---

## II. Implementation

### 2.1 Topology
- **Cloud Provider**: Google Cloud Platform (GCP)
- **Cluster Name**: `ims-cluster`
- **Cluster Type**: GKE Standard (Zonal)
- **Region / Zone**: `asia-southeast1-a` (Singapore)
- **Kubernetes Version**: `v1.36.4-gke.2046003`
- **Nodes**:
  - **Master (Control-Plane)**: GKE-managed (invisible to user, fully managed by Google)
  - **Worker**: `gke-ims-cluster-default-pool-45013d5e-ffjl` (Machine Type: `e2-standard-2`, Internal IP: `10.148.0.3`, External IP: `34.124.249.190`)
- **Image Registry**: `asia-southeast1-docker.pkg.dev/ims-k8s-project/ims-repo/`
- **Public Access**: `http://136.85.39.249` (GCP LoadBalancer)

### 2.2 Installation
Deployment was performed from a local Windows machine using `gcloud` CLI and `kubectl`. Docker images were built locally, pushed to Google Artifact Registry, and Kubernetes manifests were applied to the GKE cluster.

```
[Cluster Nodes]
PS C:\Users\Admin> kubectl get nodes -o wide
NAME                                         STATUS   ROLES    AGE   VERSION               INTERNAL-IP   EXTERNAL-IP      OS-IMAGE                             KERNEL-VERSION     CONTAINER-RUNTIME
gke-ims-cluster-default-pool-45013d5e-ffjl   Ready    <none>   97m   v1.36.4-gke.2046003   10.148.0.3    34.124.249.190   Container-Optimized OS from Google   6.12.105+ (amd64)  containerd://2.2.7
```

> *(Placeholder: Insert screenshot here showing `kubectl get nodes` and `kubectl get pods -n ims` demonstrating all pods in `Running` status)*

### 2.3 Configuration
#### Host Device Specifications (Deployment Machine)
- **Device Name**: `LAPTOP-7M0AV8S0`
- **Operating System**: Microsoft Windows 11 Home Single Language (Build 26100)
- **Processor**: 12th Gen Intel(R) Core(TM) i5-12450HX
- **Memory**: 12 GB RAM
- **Container Engine**: Docker Desktop 28.3.3 (WSL2 Backend)
- **Cloud CLI**: Google Cloud SDK (`gcloud`) + `kubectl` (v1.32.2)

#### Cloud Infrastructure
- **GCP Project**: `ims-k8s-project`
- **GKE Cluster**: `ims-cluster` (1 managed master + 1 worker node)
- **Worker VM**: `e2-standard-2` (2 vCPU, 8 GB RAM, 30 GB disk)
- **Container Registry**: Google Artifact Registry (`asia-southeast1`)

#### Kubernetes Resources in `ims` Namespace:
```
PS C:\Users\Admin> kubectl get pods,svc,pvc -n ims
NAME                            READY   STATUS    RESTARTS   AGE
pod/backend-d6bb9955-q7ctp      1/1     Running   0          11m
pod/frontend-76cb9f4f94-cj287   1/1     Running   0          11m
pod/mysql-0                     1/1     Running   0          12m

NAME               TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)          AGE
service/backend    NodePort       34.118.229.129   <none>          5050:30050/TCP   11m
service/frontend   LoadBalancer   34.118.234.73    136.85.39.249   80:31646/TCP     11m
service/mysql      ClusterIP      34.118.238.47    <none>          3306/TCP         12m

NAME                              STATUS   VOLUME                                     CAPACITY   ACCESS MODES
persistentvolumeclaim/mysql-pvc   Bound    pvc-6c21c6fd-f2ce-4bd9-b442-722b709840c9   1Gi        RWO
```

> *(Placeholder: Insert screenshot here showing the GKE console or PowerShell running `kubectl get all -n ims`)*

---

## III. Results

### 3.1 Scenario 1: Authentication and Session Creation
- **Objective**: Verify that user login executes dynamically through the GCP LoadBalancer to Nginx reverse proxy to Spring Boot, checks credentials in MySQL, and receives a JWT token.
- **Test User**: `admin@gmail.com`
- **Result**:
  - Request: `POST http://136.85.39.249/api/auth/login`
  - HTTP Status: `200 OK`
  - Response:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "role": "ADMIN",
      "email": "admin@gmail.com"
    }
    ```
- **Verification**: The user interface logs in and transitions from `/login` to `/dashboard`.

> *(Placeholder: Insert screenshot here showing the login screen and successful redirect to the Dashboard)*

### 3.2 Scenario 2: Dynamic Dashboard Metrics Retrieval
- **Objective**: Verify that secure data endpoints populate live statistics from MySQL.
- **Request**: `GET http://136.85.39.249/api/dashboard/stats` with `Authorization: Bearer <TOKEN>`
- **Result**:
  - HTTP Status: `200 OK`
  - Financial, Inventory, and Alert data points query live database aggregates and display charts for Sales and Warehouse Utilization.

> *(Placeholder: Insert screenshot here of the main Inventory Dashboard with charts rendered)*

### 3.3 Scenario 3: Inventory Data Mutation (CRUD Transaction)
- **Objective**: Verify dynamic write operations across tiers.
- **Action**: Creating a new Product / Supplier or modifying stock levels.
- **Result**:
  - Client dispatches API call to backend.
  - Backend performs transaction commit inside `mysql-0`.
  - Changes persist to `mysql-pvc` (GCE Persistent Disk) across pod restarts and instantly reflect in UI tables.

> *(Placeholder: Insert screenshot here of the Products list showing records loaded from the database)*

### 3.4 Scenario 4: Public Internet Accessibility
- **Objective**: Verify the application is accessible from any device connected to the internet without any software installation.
- **Test**: A friend opens `http://136.85.39.249` on their phone or laptop browser.
- **Result**:
  - The login page loads immediately.
  - Login with `admin@gmail.com` / `123456` succeeds.
  - Dashboard, Products, Transactions all render with live data.
- **Conclusion**: The GKE deployment is publicly accessible. No VPN, Docker, or Kubernetes knowledge is required to use the application.

> *(Placeholder: Insert screenshot here of the app opened on a different device or phone browser)*
