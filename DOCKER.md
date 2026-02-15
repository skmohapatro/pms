# Docker Deployment Guide

This guide explains how to run the Investment Portfolio Management System using Docker on any machine.

## Prerequisites

Before you begin, ensure you have the following installed on your machine:

### 1. Docker Desktop

**Windows:**
- Download from: https://docs.docker.com/desktop/install/windows-install/
- Requires Windows 10/11 64-bit with WSL 2 enabled
- After installation, restart your computer

**macOS:**
- Download from: https://docs.docker.com/desktop/install/mac-install/
- Choose the correct version for your chip (Intel or Apple Silicon)

**Linux (Ubuntu/Debian):**
```bash
# Update package index
sudo apt-get update

# Install Docker
sudo apt-get install docker.io docker-compose

# Add your user to docker group (to run without sudo)
sudo usermod -aG docker $USER

# Log out and log back in for changes to take effect
```

### 2. Verify Installation

Open a terminal/command prompt and run:
```bash
docker --version
docker-compose --version
```

You should see version numbers for both commands.

---

## Step-by-Step Deployment

### Step 1: Get the Source Code

Clone or copy the project to your machine:

```bash
# If using Git
git clone <repository-url>
cd pms

# Or copy the project folder to your desired location
```

### Step 2: Navigate to Project Directory

Open a terminal/command prompt and navigate to the project root:

```bash
cd /path/to/pms
```

Make sure you can see the `docker-compose.yml` file in this directory:
```bash
# Windows
dir docker-compose.yml

# Linux/macOS
ls docker-compose.yml
```

### Step 3: Build and Start the Containers

Run the following command to build and start both frontend and backend:

```bash
docker-compose up --build
```

**First-time build may take 5-10 minutes** as it downloads base images and dependencies.

You will see logs from both containers. Wait until you see:
- Backend: `Started PortfolioApplication in X seconds`
- Frontend: `nginx` is running

### Step 4: Access the Application

Open your web browser and navigate to:

| Service | URL |
|---------|-----|
| **Frontend (Main App)** | http://localhost:4200 |
| **Backend API** | http://localhost:8080/api |
| **H2 Database Console** | http://localhost:8080/h2-console |
| **API Documentation** | http://localhost:8080/swagger-ui.html |

### Step 5: Verify Everything Works

1. Open http://localhost:4200
2. You should see the Portfolio Management dashboard
3. Try uploading an Excel file or adding a purchase entry

---

## Common Commands

### Run in Background (Detached Mode)
```bash
docker-compose up -d --build
```

### View Logs
```bash
# All services
docker-compose logs -f

# Backend only
docker-compose logs -f backend

# Frontend only
docker-compose logs -f frontend
```

### Stop the Application
```bash
# Stop but keep containers
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop, remove containers, and delete data volumes
docker-compose down -v
```

### Restart Services
```bash
docker-compose restart
```

### Rebuild After Code Changes
```bash
docker-compose up --build
```

### Check Container Status
```bash
docker-compose ps
```

---

## Troubleshooting

### Port Already in Use

**Error:** `Bind for 0.0.0.0:4200 failed: port is already allocated`

**Solution:** Another application is using port 4200 or 8080. Either:
1. Stop the other application, or
2. Change the port in `docker-compose.yml`:
   ```yaml
   ports:
     - "3000:80"  # Change 4200 to 3000
   ```

### Docker Daemon Not Running

**Error:** `Cannot connect to the Docker daemon`

**Solution:**
- Windows/macOS: Start Docker Desktop application
- Linux: `sudo systemctl start docker`

### Build Fails - Out of Memory

**Error:** Build process killed or hangs

**Solution:** Increase Docker memory allocation:
- Docker Desktop > Settings > Resources > Memory > Set to at least 4GB

### Cannot Access Application

**Issue:** Browser shows "Connection refused"

**Solutions:**
1. Wait a bit longer - backend may still be starting
2. Check if containers are running: `docker-compose ps`
3. Check logs for errors: `docker-compose logs`

### Database Data Lost After Restart

**Issue:** Data disappears after `docker-compose down`

**Solution:** Data persists by default in a Docker volume. If you used `docker-compose down -v`, the volume is deleted. To preserve data, use only `docker-compose down` without `-v`.

---

## Configuration

### Changing Ports

Edit `docker-compose.yml`:

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

Backend environment variables can be set in `docker-compose.yml`:

```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_DATASOURCE_URL=jdbc:h2:file:./data/portfoliodb
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                                                          │
│  ┌──────────────┐         ┌──────────────────────────┐  │
│  │   Frontend   │         │        Backend           │  │
│  │   (nginx)    │ ──────► │    (Spring Boot)         │  │
│  │   Port: 80   │  /api/* │      Port: 8080          │  │
│  └──────────────┘         │                          │  │
│         │                 │  ┌────────────────────┐  │  │
│         │                 │  │   H2 Database      │  │  │
│         │                 │  │   (File-based)     │  │  │
│         │                 │  └────────────────────┘  │  │
│         │                 └──────────────────────────┘  │
└─────────│───────────────────────────│────────────────────┘
          │                           │
          ▼                           ▼
    localhost:4200              localhost:8080
```

---

## Production Considerations

For production deployment, consider:

1. **Use a proper database** instead of H2 (PostgreSQL, MySQL)
2. **Enable HTTPS** with SSL certificates
3. **Set secure passwords** for database
4. **Use Docker secrets** for sensitive configuration
5. **Set up health checks** and monitoring
6. **Configure proper logging**
7. **Use a reverse proxy** like Traefik or nginx-proxy

---

## Quick Reference

| Action | Command |
|--------|---------|
| Start | `docker-compose up -d --build` |
| Stop | `docker-compose down` |
| Logs | `docker-compose logs -f` |
| Status | `docker-compose ps` |
| Rebuild | `docker-compose up --build` |
| Clean all | `docker-compose down -v --rmi all` |
