######################################################################
# deploy-k8s.ps1 - Deploy Inventory Management System to kind (K8s)
#
# Prerequisites:
#   1. Docker Desktop running
#   2. kind.exe (bundled or in PATH)
#   3. kubectl installed
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File deploy-k8s.ps1
######################################################################
$ErrorActionPreference = "Continue"

$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path
$K8S  = Join-Path $ROOT "k8s"
$BE   = Join-Path $ROOT "Backend"
$FE   = Join-Path $ROOT "frontend"
$DB   = Join-Path $ROOT "database"

# Ensure local project folder & winget path are in current session PATH
$env:Path = "$ROOT;C:\Users\Admin\AppData\Local\Microsoft\WinGet\Packages\Kubernetes.kind_Microsoft.Winget.Source_8wekyb3d8bbwe;$env:Path"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  IMS Kubernetes Deployment - kind"          -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# -- Step 0: Verify prerequisites --
Write-Host "[0/7] Checking prerequisites..." -ForegroundColor Yellow

$kindCmd = Get-Command "kind.exe" -ErrorAction SilentlyContinue
if (-not $kindCmd) {
    Write-Error "kind.exe was not found. Please ensure kind.exe is placed in $ROOT"
    exit 1
}

$kubectlCmd = Get-Command "kubectl.exe" -ErrorAction SilentlyContinue
if (-not $kubectlCmd) {
    Write-Error "kubectl.exe was not found in PATH."
    exit 1
}

$dockerCmd = Get-Command "docker.exe" -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Error "docker.exe was not found in PATH."
    exit 1
}

$dockerRunning = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Desktop is not running. Please start Docker Desktop and wait until it is ready."
    exit 1
}

Write-Host '  OK - docker, kind, kubectl found and Docker is running' -ForegroundColor Green

# -- Step 1: Create kind cluster --
Write-Host ""
Write-Host '[1/7] Creating kind cluster - 1 master + 1 worker ...' -ForegroundColor Yellow

$clusterList = & kind get clusters 2>$null
if ($clusterList -contains "ims-cluster") {
    Write-Host '  Cluster ims-cluster already exists, reusing it.' -ForegroundColor DarkYellow
} else {
    & kind create cluster --config "$K8S\kind-cluster.yaml" --wait 120s
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create kind cluster."
        exit 1
    }
    Write-Host '  OK - Cluster created' -ForegroundColor Green
}

& kubectl cluster-info --context kind-ims-cluster

# -- Step 2: Build Docker images --
Write-Host ""
Write-Host '[2/7] Building backend Docker image...' -ForegroundColor Yellow
docker build -t ims-backend:latest "$BE"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to build backend image."
    exit 1
}
Write-Host '  OK - ims-backend:latest built' -ForegroundColor Green

Write-Host ""
Write-Host '[3/7] Building frontend Docker image...' -ForegroundColor Yellow
docker build -t ims-frontend:latest --build-arg REACT_APP_API_BASE_URL=/api "$FE"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to build frontend image."
    exit 1
}
Write-Host '  OK - ims-frontend:latest built' -ForegroundColor Green

# -- Step 3: Load images into kind --
Write-Host ""
Write-Host '[4/7] Loading images into kind cluster...' -ForegroundColor Yellow
& kind load docker-image ims-backend:latest  --name ims-cluster
& kind load docker-image ims-frontend:latest --name ims-cluster
Write-Host '  OK - Images loaded' -ForegroundColor Green

# -- Step 4: Create namespace, secrets, configmaps --
Write-Host ""
Write-Host '[5/7] Applying K8s namespace, secrets, configmaps...' -ForegroundColor Yellow
& kubectl apply -f "$K8S\00-namespace.yaml"
& kubectl apply -f "$K8S\01-secrets.yaml"
& kubectl apply -f "$K8S\02-configmaps.yaml"
Write-Host '  OK - Namespace and config applied' -ForegroundColor Green

# -- Step 5: Create MySQL init ConfigMap from SQL files --
Write-Host ""
Write-Host '[6/7] Creating MySQL init ConfigMap from SQL files...' -ForegroundColor Yellow
& kubectl create configmap mysql-init `
  --from-file="01-init.sql=$DB\inventory_db.sql" `
  --namespace=ims `
  --dry-run=client -o yaml | & kubectl apply -f -
Write-Host '  OK - mysql-init ConfigMap created' -ForegroundColor Green

# -- Step 6: Deploy all services --
Write-Host ""
Write-Host '[7/7] Deploying MySQL, Backend, Frontend...' -ForegroundColor Yellow
& kubectl apply -f "$K8S\03-mysql.yaml"

Write-Host '  Waiting for MySQL to be ready...'
& kubectl rollout status statefulset/mysql -n ims --timeout=180s

& kubectl apply -f "$K8S\04-backend.yaml"
Write-Host '  Waiting for Backend to be ready...'
& kubectl rollout status deployment/backend -n ims --timeout=300s

& kubectl apply -f "$K8S\05-frontend.yaml"
Write-Host '  Waiting for Frontend to be ready...'
& kubectl rollout status deployment/frontend -n ims --timeout=120s

# -- Done! --
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  DEPLOYMENT COMPLETE!"                       -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host '  Frontend:   http://localhost:30000' -ForegroundColor Cyan
Write-Host '  Backend:    http://localhost:30050' -ForegroundColor Cyan
Write-Host '  Swagger UI: http://localhost:30050/swagger-ui/index.html' -ForegroundColor Cyan
Write-Host ""
Write-Host '  Test accounts:' -ForegroundColor White
Write-Host '    admin1@gmail.com   / 12345678' -ForegroundColor White
Write-Host '    manager1@gmail.com / 12345678' -ForegroundColor White
Write-Host '    staff1@gmail.com   / 12345678' -ForegroundColor White
Write-Host ""
Write-Host '  Useful commands:' -ForegroundColor DarkGray
Write-Host '    kubectl get all -n ims' -ForegroundColor DarkGray
Write-Host '    kubectl get nodes' -ForegroundColor DarkGray
Write-Host '    kubectl logs -n ims deployment/backend -f' -ForegroundColor DarkGray
Write-Host '    kubectl logs -n ims deployment/frontend -f' -ForegroundColor DarkGray
Write-Host '    kind delete cluster --name ims-cluster' -ForegroundColor DarkGray
Write-Host ""
