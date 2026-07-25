# Calefaction Deployment Script for Docker Desktop K8s

Write-Host "Starting deployment process..." -ForegroundColor Cyan

# 1. Build the application
Write-Host "`nStep 1: Building JAR with Gradle..." -ForegroundColor Yellow
./gradlew build
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed!"
    exit $LASTEXITCODE
}

# 2. Build the Docker image
Write-Host "`nStep 2: Building Docker image..." -ForegroundColor Yellow
docker build -t calefaction:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker build failed!"
    exit $LASTEXITCODE
}

# 3. Apply Kubernetes manifests
Write-Host "`nStep 3: Applying Kubernetes manifests..." -ForegroundColor Yellow

Write-Host "Applying secrets..."
kubectl apply -f k8s/secrets.yaml

Write-Host "Applying deployment..."
kubectl apply -f k8s/deployment.yaml

# 4. Force a rollout restart to ensure the new image is used
Write-Host "`nStep 4: Restarting deployment..." -ForegroundColor Yellow
kubectl rollout restart deployment/calefaction

Write-Host "`n[Deployment complete] Checking pod status..." -ForegroundColor Green
kubectl get pods -l app=calefaction
