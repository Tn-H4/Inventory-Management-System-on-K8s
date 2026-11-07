# Inventory Management System - Kubernetes Deployment Report

---

## I. Introduction

### 1.1 Overview
The **Inventory Management System (IMS)** is a full-stack, dynamic web application designed to handle enterprise warehouse operations. It provides features including product stock management, supplier coordination, purchase/sell/return transaction recording, multi-role approval proposals (Admin, Manager, Staff), automated email notifications, audit logs, and analytical dashboards.

This application is **fully dynamic**:
- Client-side views query a live REST API asynchronously.
- State, inventory counts, audit records, and auth tokens are persisted and evaluated dynamically against a transactional relational database (MySQL 8.0).
- Backend business logic, access control, and dynamic reports are processed in real-time by a Spring Boot application runtime.

### 1.2 Architecture
The system follows a multi-tier microservice-style containerized architecture deployed on Kubernetes:

```
[ User Browser ]
       │ :30000 (NodePort)
       ▼
┌────────────────────────────────────────────────────────┐
│  Kubernetes Worker Node (ims-cluster-worker)          │
│                                                        │
│  ┌─────────────────────────┐                           │
│  │  Frontend Pod           │                           │
│  │  • Nginx Alpine Server  │                           │
│  │  • React 19 SPA Build   │                           │
│  │  • Reverse Proxy (/api) │                           │
│  └───────────┬─────────────┘                           │
│              │ http://backend:5050                     │
│              ▼                                         │
│  ┌─────────────────────────┐                           │
│  │  Backend Pod            │                           │
│  │  • Spring Boot 3.3.5    │                           │
│  │  • Java 21 LTS          │                           │
│  │  • JWT Auth & Security  │                           │
│  └───────────┬─────────────┘                           │
│              │ jdbc:mysql://mysql:3306                 │
│              ▼                                         │
│  ┌─────────────────────────┐   ┌────────────────────┐  │
│  │  Database Pod (mysql-0) │──▶│ PersistentVolume   │  │
│  │  • MySQL 8.0 Engine     │   │ Claim (1Gi RWO)    │  │
│  └─────────────────────────┘   └────────────────────┘  │
└────────────────────────────────────────────────────────┘
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
   - Bound to a `PersistentVolumeClaim` (`mysql-pvc`) to ensure data durability across pod restarts.
   - Initialized via a Kubernetes `ConfigMap` (`mysql-init`) containing pre-configured schema tables and seed data.

### 1.3 Operation

#### Inter-Service Communication
- **Frontend ➔ Backend**: When users perform actions, React dispatches Axios requests to `/api/...`. Nginx intercepts these paths and reverse-proxies them internally to `http://backend:5050/api/` via CoreDNS service discovery.
- **Backend ➔ Database**: The Spring Boot service connects to MySQL using JDBC over port `3306` (`jdbc:mysql://mysql:3306/inventory_db`).

#### Docker Image Creation
Both Docker images use multi-stage builds to minimize image sizes and enhance security:
- **Backend (`Backend/Dockerfile`)**:
  - *Stage 1 (Build)*: Uses `eclipse-temurin:21-jdk-alpine`, pulls dependencies via Maven wrapper (`./mvnw`), and compiles code into a `.jar` package (`mvn package -DskipTests`).
  - *Stage 2 (Runtime)*: Uses lightweight `eclipse-temurin:21-jre-alpine` and copies only `app.jar` to run.
- **Frontend (`frontend/Dockerfile`)**:
  - *Stage 1 (Build)*: Uses `node:20-alpine`, installs NPM dependencies (`npm ci`), and creates an optimized static bundle (`npm run build`).
  - *Stage 2 (Runtime)*: Copies static production artifacts into `nginx:alpine` and mounts a custom reverse-proxy `nginx.conf`.

#### Master / Worker Node Communication
The deployment runs on a 2-node **kind** (Kubernetes-in-Docker) cluster:
- **Control-Plane (Master Node - `ims-cluster-control-plane`)**: Runs the Kubernetes Control Plane components: `kube-apiserver`, `etcd`, `kube-controller-manager`, and `kube-scheduler`.
- **Worker Node (`ims-cluster-worker`)**: Runs `kubelet`, `containerd`, and `kube-proxy`.
- **How they interact**:
  1. `kubectl` applies manifests to the `kube-apiserver` running on the Master node.
  2. The `kube-scheduler` assigns the pods (`mysql-0`, `backend`, `frontend`) to the Worker node based on resource availability.
  3. The Worker node's `kubelet` receives the assignment, talks to `containerd` to run the containers, and configures overlay networking via CNI.
  4. NodePort mapping forwards traffic from the host machine directly through the kind network bridge into the pods on the worker node.

---

## II. Implementation

### 2.1 Topology
- **Cluster Name**: `ims-cluster`
- **Kubernetes Version**: `v1.37.0`
- **Nodes**:
  - `ims-cluster-control-plane` (Role: `control-plane`, IP: `172.18.0.2`)
  - `ims-cluster-worker` (Role: `<none>` / worker, IP: `172.18.0.3`)
- **Port Mapping**:
  - Host Port `30000` ➔ Worker NodePort `30000` (Frontend Nginx)
  - Host Port `30050` ➔ Worker NodePort `30050` (Backend Spring Boot API)

### 2.2 Installation
Execution of deployment automation via `deploy-k8s.ps1` initializes the cluster, compiles container images, and mounts manifests into the namespace `ims`.

```
[Installation Terminal Output]
PS C:\Users\Admin\Downloads\inventory-management-master\inventory-management-master> kubectl get nodes -o wide
NAME                        STATUS   ROLES           AGE   VERSION   INTERNAL-IP   CONTAINER-RUNTIME
ims-cluster-control-plane   Ready    control-plane   1h    v1.37.0   172.18.0.2    containerd://2.3.4
ims-cluster-worker          Ready    <none>          1h    v1.37.0   172.18.0.3    containerd://2.3.4
```

> *(Placeholder: Insert screenshot here showing `kubectl get nodes` and `kubectl get pods -n ims` demonstrating all pods in `Running` status)*

### 2.3 Configuration
#### Host Device Specifications
- **Device Name**: `LAPTOP-7M0AV8S0`
- **Operating System**: Microsoft Windows 11 Home Single Language (Build 26100)
- **Processor**: 12th Gen Intel(R) Core(TM) i5-12450HX (16 CPUs)
- **Memory**: 12 GB RAM
- **Container Engine**: Docker Desktop 28.3.3 (WSL2 Backend)
- **Kubernetes Engine**: kind (v0.27.0) with client `kubectl` (v1.32.2)

#### Kubernetes Resources in `ims` Namespace:
```
PS C:\Users\Admin> kubectl get pods,svc,pvc -n ims
NAME                            READY   STATUS    RESTARTS   AGE
pod/backend-65d4f8c74d-s426r    1/1     Running   0          25m
pod/frontend-5d8cd987d4-mkfmz   1/1     Running   0          75m
pod/mysql-0                     1/1     Running   0          75m

NAME               TYPE        CLUSTER-IP     PORT(S)          AGE
service/backend    NodePort    10.96.79.206   5050:30050/TCP   75m
service/frontend   NodePort    10.96.122.20   80:30000/TCP     75m
service/mysql      ClusterIP   10.96.55.242   3306/TCP         75m

NAME                              STATUS   VOLUME                                     CAPACITY   ACCESS MODES
persistentvolumeclaim/mysql-pvc   Bound    pvc-d84b6df8-2a49-40ab-a148-3f525861b9b6   1Gi        RWO
```

> *(Placeholder: Insert screenshot here showing PowerShell running `kubectl get all -n ims` along with system info)*

---

## III. Results

### 3.1 Scenario 1: Authentication and Session Creation
- **Objective**: Verify that user login executes dynamically through Nginx reverse proxy to Spring Boot, checks credentials in MySQL, and receives a JWT token.
- **Test User**: `admin@gmail.com`
- **Result**:
  - Request: `POST http://localhost:30000/api/auth/login`
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
- **Request**: `GET http://localhost:30000/api/dashboard/stats` with `Authorization: Bearer <TOKEN>`
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
  - Changes persist to `mysql-pvc` across pod restarts and instantly reflect in UI tables.

> *(Placeholder: Insert screenshot here of the Products list showing records loaded from the database)*

