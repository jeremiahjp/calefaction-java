# Calefaction Discord Bot

A modern Discord bot built with Java 21, Spring Boot, and JDA, designed to run on Kubernetes.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) with **Kubernetes enabled**.
- Java 21 or higher (for local development).
- `kubectl` CLI.

---

## 🛠️ Building the Application

### 1. Build the JAR
Build the application locally to ensure it compiles correctly:
```bash
./gradlew build
```

### 2. Build the Docker Image
The project includes a multi-stage `Dockerfile` that builds and packages the application:
```bash
docker build -t calefaction:latest .
```

---

## 🚀 Deploying to Kubernetes (Docker Desktop)

### Option 1: Automated Deployment (Windows)
If you are on Windows, you can use the included PowerShell script to automatically build the JAR, build the Docker image, apply K8s manifests, and restart the deployment in one go:
```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1
```

### Option 2: Manual Deployment

### 1. Configure Secrets
Open `k8s/secrets.yaml` and fill in your API keys (Plaintext is fine as the file uses `stringData`).

Apply the secrets to your cluster:
```bash
kubectl apply -f k8s/secrets.yaml
```

### 2. Deploy the Bot
Apply the deployment manifest:
```bash
kubectl apply -f k8s/deployment.yaml
```

---

## 🔍 Verification

Check if the pod is running:
```bash
kubectl get pods
```

View the bot's logs to ensure it connected to Discord successfully:
```bash
kubectl logs -l app=calefaction -f
```

---

## 🔄 Updating the Bot

When you make code changes, you can either run the `deploy.ps1` script again, or manually:
1. Re-build the Docker image: `docker build -t calefaction:latest .`
2. Restart the deployment to pick up the new image:
```bash
kubectl rollout restart deployment/calefaction
```

## ⚙️ Configuration

All configuration is managed in `src/main/resources/application.yml`:

- **LLM Providers & Models**: You can add or remove models (OpenAI, Claude, Gemini, Grok) without changing the code.
- **Admin Access**: To use restricted commands (like video generation or dev-mode games), add your Discord User ID to the `bot.admin-user-ids` list.
