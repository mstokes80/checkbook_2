# Checkbook - Quick Start Guide

## TL;DR - Deploy in 5 Minutes

### On Your Ubuntu Server

```bash
# 1. Install Docker (if not installed)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 2. Upload and extract project
# (Transfer checkbook-app.tar.gz to server first)
mkdir -p ~/checkbook && cd ~/checkbook
tar -xzf ~/checkbook-app.tar.gz

# 3. Set up environment
cp .env.prod.example .env.prod
nano .env.prod  # Update passwords and secrets

# 4. Deploy
chmod +x scripts/deploy/deploy.sh
./scripts/deploy/deploy.sh
```

### On Client Devices

1. **Add to hosts file** (replace `<SERVER_IP>` with your server's IP):
   ```bash
   echo "<SERVER_IP> checkbook.local-stokesnet.net" | sudo tee -a /etc/hosts
   ```

2. **Install mkcert CA** (for trusted certificates):
   ```bash
   # macOS
   brew install mkcert && mkcert -install

   # Linux
   sudo apt install libnss3-tools
   curl -JLO "https://dl.filippo.io/mkcert/latest?for=linux/amd64"
   chmod +x mkcert-v*-linux-amd64
   sudo mv mkcert-v*-linux-amd64 /usr/local/bin/mkcert
   mkcert -install
   ```

3. **Copy CA certificate from server**:
   ```bash
   scp USER@SERVER_IP:~/.local/share/mkcert/rootCA.pem .
   # Then install rootCA.pem in your system's trust store
   ```

4. **Access the app**:
   - Open: `https://checkbook.local-stokesnet.net`

## Required Environment Variables

Edit `.env.prod` and set these minimum values:

```env
# Strong database password
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# Strong Redis password
REDIS_PASSWORD=$(openssl rand -base64 32)

# Strong JWT secret (64 characters minimum)
JWT_SECRET=$(openssl rand -base64 64)

# Your SMTP settings
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

## Essential Commands

```bash
# Start services
docker compose -f docker-compose.prod.yml up -d

# Stop services
docker compose -f docker-compose.prod.yml down

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Restart a service
docker compose -f docker-compose.prod.yml restart api

# Backup database
./scripts/backup/backup-database.sh

# Check status
docker compose -f docker-compose.prod.yml ps
```

## Accessing Services

- **Web Application**: https://checkbook.local-stokesnet.net
- **API Health Check**: https://checkbook.local-stokesnet.net/api/health
- **Direct API**: https://checkbook.local-stokesnet.net/api

## Default Port Mapping

- **80** → HTTP (redirects to HTTPS)
- **443** → HTTPS (nginx)
- **8080** → API (internal)
- **5432** → PostgreSQL (internal)
- **6379** → Redis (internal)

## Troubleshooting

### Services won't start?
```bash
docker compose -f docker-compose.prod.yml logs
```

### Can't access website?
```bash
# Check nginx is running
docker ps | grep nginx

# Test locally on server
curl -k https://localhost

# Check firewall
sudo ufw status
```

### Certificate not trusted?
```bash
# Verify cert exists
ls -l nginx/ssl/

# Re-run mkcert setup
./scripts/ssl/setup-mkcert.sh
```

### Database issues?
```bash
# Access database
docker exec -it checkbook-postgres-prod psql -U checkbook_user -d checkbook_prod

# View database logs
docker logs checkbook-postgres-prod
```

## Full Documentation

See [DEPLOYMENT.md](./DEPLOYMENT.md) for complete deployment guide.

## Security Checklist

Before going to production:

- [ ] Changed all default passwords
- [ ] Generated strong JWT secret (64+ chars)
- [ ] Configured real SMTP server
- [ ] Set up regular database backups
- [ ] Configured firewall (UFW)
- [ ] Updated server packages
- [ ] Tested backup/restore procedure

## Support

Check logs first, then review:
- [DEPLOYMENT.md](./DEPLOYMENT.md) - Full deployment guide
- [README.md](./README.md) - Application documentation