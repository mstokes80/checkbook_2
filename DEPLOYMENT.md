# Checkbook Application - Production Deployment Guide

This guide covers deploying the Checkbook application on an Ubuntu Linux server using Docker Compose with mkcert SSL certificates for local network access.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Initial Setup](#initial-setup)
- [SSL Certificate Setup](#ssl-certificate-setup)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Post-Deployment](#post-deployment)
- [Maintenance](#maintenance)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Server Requirements

- Ubuntu Linux 20.04 LTS or newer
- Docker 20.10+ and Docker Compose V2
- Minimum 2GB RAM, 20GB disk space
- Network access on your local network

### Install Docker on Ubuntu Server

```bash
# Update package index
sudo apt update

# Install dependencies
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common

# Add Docker's GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Add your user to docker group
sudo usermod -aG docker $USER

# Verify installation
docker --version
docker compose version
```

## Initial Setup

### 1. Transfer Project Files to Server

From your local machine:

```bash
# Create a tarball excluding node_modules, target, and other build artifacts
cd /path/to/Checkbook2
tar -czf checkbook-app.tar.gz \
  --exclude='node_modules' \
  --exclude='target' \
  --exclude='.git' \
  --exclude='*.log' \
  --exclude='dist' \
  --exclude='build' \
  .

# Copy to server (replace USER and SERVER_IP)
scp checkbook-app.tar.gz USER@SERVER_IP:~/
```

On the server:

```bash
# Extract files
mkdir -p ~/checkbook
cd ~/checkbook
tar -xzf ~/checkbook-app.tar.gz
rm ~/checkbook-app.tar.gz
```

### 2. Set Up DNS/Hosts File

On your Ubuntu server, add an entry to `/etc/hosts`:

```bash
echo "127.0.0.1 checkbook.local-stokesnet.net" | sudo tee -a /etc/hosts
```

On all client devices that will access the application, add:

```bash
# Replace <SERVER_IP> with your Ubuntu server's IP address
<SERVER_IP> checkbook.local-stokesnet.net
```

## SSL Certificate Setup

### Option 1: Use Pre-Generated Certificates (Recommended)

If you've already generated mkcert certificates on your development machine:

1. Copy the certificates from your local machine to the server:

```bash
# From your local machine
scp nginx/ssl/cert.pem USER@SERVER_IP:~/checkbook/nginx/ssl/
scp nginx/ssl/key.pem USER@SERVER_IP:~/checkbook/nginx/ssl/
```

2. Install mkcert CA on all client devices:

```bash
# On macOS
brew install mkcert
mkcert -install

# On Linux
sudo apt install libnss3-tools
wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64
chmod +x mkcert-v1.4.4-linux-amd64
sudo mv mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert
mkcert -install
```

3. Copy the CA root certificate to client devices and trust it.

### Option 2: Generate Certificates on Server

If mkcert is available on the server:

```bash
cd ~/checkbook
./scripts/ssl/setup-mkcert.sh
```

## Configuration

### 1. Create Production Environment File

```bash
cd ~/checkbook
cp .env.prod.example .env.prod
```

### 2. Edit `.env.prod` with Secure Values

```bash
nano .env.prod
```

**IMPORTANT**: Update these values:

```env
# Database
POSTGRES_PASSWORD=<generate-strong-password>

# Redis
REDIS_PASSWORD=<generate-strong-password>

# JWT Secret (generate with: openssl rand -base64 64)
JWT_SECRET=<generate-strong-secret-key>

# Email (configure your SMTP settings)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=<your-app-specific-password>
MAIL_FROM=noreply@checkbook.local-stokesnet.net
```

**Generate Strong Passwords:**

```bash
# For passwords
openssl rand -base64 32

# For JWT Secret
openssl rand -base64 64
```

## Deployment

### Initial Deployment

```bash
cd ~/checkbook

# Make scripts executable
chmod +x scripts/deploy/deploy.sh
chmod +x scripts/backup/backup-database.sh

# Run deployment script
./scripts/deploy/deploy.sh
```

The script will:
1. Verify environment configuration
2. Build Docker images
3. Start all services
4. Run health checks

### Manual Deployment Steps

If you prefer to deploy manually:

```bash
cd ~/checkbook

# Build images
docker compose -f docker-compose.prod.yml --env-file .env.prod build

# Start services
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# View logs
docker compose -f docker-compose.prod.yml logs -f
```

## Post-Deployment

### 1. Verify Services are Running

```bash
docker compose -f docker-compose.prod.yml ps
```

Expected output:
```
NAME                       STATUS              PORTS
checkbook-api-prod         Up (healthy)        8080/tcp
checkbook-nginx-prod       Up (healthy)        0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
checkbook-postgres-prod    Up (healthy)        5432/tcp
checkbook-redis-prod       Up (healthy)        6379/tcp
checkbook-ui-prod          Up (healthy)        80/tcp
```

### 2. Test Application Access

From a client device with the mkcert CA installed:

1. Open browser to: `https://checkbook.local-stokesnet.net`
2. You should see the login page with a trusted certificate
3. Test API health: `https://checkbook.local-stokesnet.net/api/health`

### 3. Create First User

Navigate to the registration page and create your admin account.

## Maintenance

### View Logs

```bash
# All services
docker compose -f docker-compose.prod.yml logs -f

# Specific service
docker compose -f docker-compose.prod.yml logs -f api

# Last 100 lines
docker compose -f docker-compose.prod.yml logs --tail=100
```

### Restart Services

```bash
# Restart all
docker compose -f docker-compose.prod.yml restart

# Restart specific service
docker compose -f docker-compose.prod.yml restart api
```

### Stop Services

```bash
docker compose -f docker-compose.prod.yml down
```

### Update Application

```bash
# Pull latest code
cd ~/checkbook
git pull  # If using git

# Rebuild and restart
./scripts/deploy/deploy.sh
```

### Database Backup

```bash
# Manual backup
./scripts/backup/backup-database.sh

# Set up automated backups (cron)
crontab -e

# Add this line for daily backups at 2 AM
0 2 * * * /home/USER/checkbook/scripts/backup/backup-database.sh >> /home/USER/checkbook/logs/backup.log 2>&1
```

### Restore Database

```bash
# Find your backup
ls -lh ~/checkbook/backups/

# Restore from backup
gunzip < ~/checkbook/backups/checkbook_backup_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i checkbook-postgres-prod psql -U checkbook_user -d checkbook_prod
```

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs

# Check specific service
docker logs checkbook-api-prod
```

### Certificate Errors

```bash
# Verify certificates exist
ls -l nginx/ssl/

# Check certificate validity
openssl x509 -in nginx/ssl/cert.pem -text -noout
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
docker exec -it checkbook-postgres-prod psql -U checkbook_user -d checkbook_prod

# Check database logs
docker logs checkbook-postgres-prod
```

### API Not Responding

```bash
# Check API logs
docker logs checkbook-api-prod

# Restart API
docker compose -f docker-compose.prod.yml restart api

# Check health endpoint
curl http://localhost:8080/api/health
```

### Clear All Data and Start Fresh

**WARNING**: This will delete all data!

```bash
# Stop and remove everything
docker compose -f docker-compose.prod.yml down -v

# Remove all volumes
docker volume rm checkbook-prod_postgres_prod_data checkbook-prod_redis_prod_data

# Redeploy
./scripts/deploy/deploy.sh
```

## Performance Tuning

### Adjust Container Resources

Edit `docker-compose.prod.yml` to add resource limits:

```yaml
services:
  api:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### Enable Nginx Caching

Add to `nginx/conf.d/checkbook.conf`:

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=100m;

location /api/ {
    proxy_cache api_cache;
    proxy_cache_valid 200 5m;
    # ... rest of config
}
```

## Security Checklist

- [x] Strong passwords for PostgreSQL and Redis
- [x] Secure JWT secret (256+ bits)
- [x] SSL/TLS enabled
- [x] Rate limiting configured
- [x] Regular database backups scheduled
- [x] Firewall configured (UFW)
- [x] Docker rootless mode (optional but recommended)
- [x] Regular security updates

### Configure UFW Firewall

```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

## Support

For issues or questions:
- Check logs first
- Review this documentation
- Check Docker and application logs
- Verify environment variables

## Quick Reference Commands

```bash
# Deploy/Update
./scripts/deploy/deploy.sh

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Backup database
./scripts/backup/backup-database.sh

# Restart services
docker compose -f docker-compose.prod.yml restart

# Stop services
docker compose -f docker-compose.prod.yml down

# Check status
docker compose -f docker-compose.prod.yml ps
```