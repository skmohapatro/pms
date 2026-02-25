# Podman Deployment Guide

This guide explains how to run the Investment Portfolio Management System using Podman on any machine.

## Prerequisites

### 1. Install Podman

**Windows:**
```powershell
# Using winget
winget install -e --id RedHat.Podman

# Or download from: https://podman.io/getting-started/installation
```

**macOS:**
```bash
# Using Homebrew
brew install podman

# Initialize and start Podman machine
podman machine init
podman machine start
```

**Linux (Ubuntu/Debian):**
```bash
# Update package index
sudo apt-get update

# Install Podman
sudo apt-get install -y podman

# Install podman-compose
pip3 install podman-compose
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install -y podman podman-compose
```

### 2. Verify Installation

```bash
podman --version
podman-compose --version
```

---

## Step-by-Step Deployment

### Step 1: Get the Source Code

```bash
# If using Git
git clone <repository-url>
cd pms

# Or copy the project folder to your desired location
```

### Step 2: Navigate to Project Directory

```bash
cd /path/to/pms
```

Verify the `podman-compose.yml` file exists:
```bash
# Windows
dir podman-compose.yml

# Linux/macOS
ls podman-compose.yml
```

### Step 3: Build and Start the Containers

```bash
podman-compose -f podman-compose.yml up --build
```

**First-time build may take 5-10 minutes** as it downloads base images and dependencies.

### Step 4: Access the Application

Open your web browser and navigate to:

| Service | URL |
|---------|-----|
| **Frontend (Main App)** | http://localhost:4200 |
| **Backend API** | http://localhost:8080/api |
| **Chat Backend** | http://localhost:5000 |
| **H2 Database Console** | http://localhost:8080/h2-console |
| **API Documentation** | http://localhost:8080/swagger-ui.html |

### Step 5: Verify Everything Works

1. Open http://localhost:4200
2. You should see the Portfolio Management dashboard
3. Try uploading an Excel file or adding a purchase entry
4. Navigate to "AI Chat" to test the chat feature

---

## Common Commands

### Run in Background (Detached Mode)
```bash
podman-compose -f podman-compose.yml up -d --build
```

### View Logs
```bash
# All services
podman-compose -f podman-compose.yml logs -f

# Backend only
podman-compose -f podman-compose.yml logs -f backend

# Frontend only
podman-compose -f podman-compose.yml logs -f frontend

# Chat backend only
podman-compose -f podman-compose.yml logs -f chat-backend
```

### Stop the Application
```bash
# Stop but keep containers
podman-compose -f podman-compose.yml stop

# Stop and remove containers
podman-compose -f podman-compose.yml down

# Stop, remove containers, and delete data volumes
podman-compose -f podman-compose.yml down -v
```

### Restart Services
```bash
podman-compose -f podman-compose.yml restart
```

### Rebuild After Code Changes
```bash
podman-compose -f podman-compose.yml up --build
```

### Check Container Status
```bash
podman-compose -f podman-compose.yml ps

# Or using podman directly
podman ps
```

### Individual Container Management
```bash
# List all containers
podman ps -a

# Stop a specific container
podman stop pms-backend

# Start a specific container
podman start pms-backend

# View logs of a specific container
podman logs -f pms-backend

# Execute command in running container
podman exec -it pms-backend /bin/sh
```

---

## Podman-Specific Features

### Rootless Mode

Podman runs in rootless mode by default, which is more secure:

```bash
# Check if running rootless
podman info | grep rootless

# Run as root (if needed)
sudo podman-compose -f podman-compose.yml up
```

### Pod Management

Podman can group containers into pods:

```bash
# List all pods
podman pod ps

# Inspect a pod
podman pod inspect <pod-name>

# Remove a pod
podman pod rm <pod-name>
```

### Generate Kubernetes YAML

Convert your setup to Kubernetes:

```bash
# Generate Kubernetes YAML from running containers
podman generate kube pms-backend > backend-k8s.yaml
```

---

## Troubleshooting

### Port Already in Use

**Error:** `Error: cannot listen on the TCP port: address already in use`

**Solution:**
1. Stop the other application using the port
2. Or change the port in `podman-compose.yml`:
   ```yaml
   ports:
     - "3000:80"  # Change 4200 to 3000
   ```

### Podman Machine Not Running (macOS/Windows)

**Error:** `Cannot connect to Podman`

**Solution:**
```bash
# Check machine status
podman machine list

# Start the machine
podman machine start

# If needed, initialize first
podman machine init
podman machine start
```

### Permission Denied

**Error:** `Permission denied while trying to connect to the Podman socket`

**Solution:**
```bash
# Add user to podman group (Linux)
sudo usermod -aG podman $USER
newgrp podman

# Or run with sudo
sudo podman-compose -f podman-compose.yml up
```

### Build Fails - Out of Memory

**Error:** Build process killed or hangs

**Solution:**
```bash
# Increase Podman machine memory (macOS/Windows)
podman machine stop
podman machine set --memory 4096
podman machine start
```

### Cannot Access Application

**Issue:** Browser shows "Connection refused"

**Solutions:**
1. Wait a bit longer - backend may still be starting
2. Check if containers are running: `podman ps`
3. Check logs for errors: `podman-compose -f podman-compose.yml logs`
4. Verify Podman machine is running (macOS/Windows): `podman machine list`

### Volume Permissions

**Issue:** Permission errors with volumes

**Solution:**
```bash
# Set correct SELinux context (Linux with SELinux)
podman unshare chown -R 1000:1000 ./data

# Or use :Z flag in volume mount
volumes:
  - backend-data:/app/data:Z
```

---

## Configuration

### Changing Ports

Edit `podman-compose.yml`:

```yaml
services:
  backend:
    ports:
      - "9090:8080"  # Change backend to port 9090
  
  frontend:
    ports:
      - "3000:80"  # Change frontend to port 3000
```

### Environment Variables

Backend environment variables can be set in `podman-compose.yml`:

```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_DATASOURCE_URL=jdbc:h2:file:./data/portfoliodb
```

### AI Chat Configuration

The AI Chat feature uses Dell internal AI models. To configure:

1. **Create a `.env` file** in the project root:
   ```bash
   # Copy from example
   cp chat-backend/.env.example .env
   ```

2. **Edit `.env`** with your credentials:
   ```
   USE_SSO=true
   CLIENT_ID=your-client-id
   CLIENT_SECRET=your-client-secret
   ```

3. **Run podman-compose** - it will automatically use the `.env` file

**Note:** The AI Chat requires access to Dell's internal network and valid authentication credentials.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│                      Podman Network (Pod)                           │
│                                                                     │
│  ┌──────────────┐    ┌──────────────────────────┐    ┌───────────┐ │
│  │   Frontend   │    │        Backend           │    │   Chat    │ │
│  │   (nginx)    │───►│    (Spring Boot)         │───►│  Backend  │ │
│  │   Port: 80   │    │      Port: 8080          │    │ (Flask)   │ │
│  └──────────────┘    │                          │    │ Port:5000 │ │
│         │            │  ┌────────────────────┐  │    └─────┬─────┘ │
│         │            │  │   H2 Database      │  │          │       │
│         │            │  │   (File-based)     │  │          │       │
│         │            │  └────────────────────┘  │          ▼       │
│         │            └──────────────────────────┘    Dell GenAI    │
└─────────│───────────────────────│────────────────────────API───────┘
          │                       │
          ▼                       ▼
    localhost:4200          localhost:8080
```

---

## Podman vs Docker Differences

| Feature | Docker | Podman |
|---------|--------|--------|
| **Daemon** | Requires Docker daemon | Daemonless |
| **Root** | Runs as root | Rootless by default |
| **Compose** | docker-compose | podman-compose |
| **Compatibility** | Docker API | Docker-compatible API |
| **Kubernetes** | Separate tool | Built-in YAML generation |

---

## Migration from Docker

If you're migrating from Docker:

```bash
# Alias podman as docker (optional)
alias docker=podman
alias docker-compose=podman-compose

# Import Docker images
podman load -i docker-image.tar

# Use existing Dockerfiles (no changes needed)
podman build -f Dockerfile -t myimage
```

---

## Production Considerations

For production deployment, consider:

1. **Use a proper database** instead of H2 (PostgreSQL, MySQL)
2. **Enable HTTPS** with SSL certificates
3. **Set secure passwords** for database
4. **Use Podman secrets** for sensitive configuration
5. **Set up health checks** and monitoring
6. **Configure proper logging**
7. **Use systemd for auto-start** (Linux)
8. **Consider Kubernetes** for orchestration

### Systemd Integration (Linux)

Generate systemd service files:

```bash
# Generate service file for a container
podman generate systemd --new --name pms-backend > ~/.config/systemd/user/pms-backend.service

# Enable and start service
systemctl --user enable pms-backend.service
systemctl --user start pms-backend.service
```

---

## Quick Reference

| Action | Command |
|--------|---------|
| Start | `podman-compose -f podman-compose.yml up -d --build` |
| Stop | `podman-compose -f podman-compose.yml down` |
| Logs | `podman-compose -f podman-compose.yml logs -f` |
| Status | `podman-compose -f podman-compose.yml ps` |
| Rebuild | `podman-compose -f podman-compose.yml up --build` |
| Clean all | `podman-compose -f podman-compose.yml down -v` |
| Remove images | `podman rmi -a` |
| Prune system | `podman system prune -a` |

---

## Additional Resources

- Podman Documentation: https://docs.podman.io/
- Podman Compose: https://github.com/containers/podman-compose
- Migration Guide: https://podman.io/getting-started/migration
