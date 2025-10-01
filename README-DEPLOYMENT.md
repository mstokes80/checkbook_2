# Deployment Configuration Summary

This document provides an overview of all deployment-related files and configurations.

## 📁 Project Structure

```
Checkbook2/
├── docker-compose.prod.yml          # Production Docker Compose configuration
├── .env.prod.example                # Production environment template
├── DEPLOYMENT.md                    # Complete deployment guide
├── QUICK_START.md                   # Quick deployment reference
├── nginx/
│   ├── nginx.conf                   # Main nginx configuration
│   ├── conf.d/
│   │   └── checkbook.conf          # Application-specific nginx config
│   ├── ssl/
│   │   ├── cert.pem                # mkcert SSL certificate
│   │   └── key.pem                 # mkcert SSL private key
│   └── logs/                       # Nginx logs
├── scripts/
│   ├── deploy/
│   │   └── deploy.sh               # Main deployment script
│   ├── ssl/
│   │   └── setup-mkcert.sh         # SSL certificate generation script
│   └── backup/
│       └── backup-database.sh      # Database backup script
├── checkbook-api/
│   └── Dockerfile                   # Production backend Dockerfile
└── checkbook-ui/
    ├── Dockerfile                   # Production frontend Dockerfile
    └── nginx.conf                   # Frontend nginx configuration
```

## 🔐 SSL/TLS Configuration

- **Domain**: `checkbook.local-stokesnet.net`
- **Certificate Tool**: mkcert (LAN-trusted certificates)
- **Certificate Location**: `nginx/ssl/`
- **Valid For**: 3 years from generation
- **Includes**: Wildcard for `*.local-stokesnet.net`, localhost, 127.0.0.1, ::1

## 🐳 Docker Services

### Production Stack (`docker-compose.prod.yml`)

| Service | Container Name | Ports | Description |
|---------|---------------|-------|-------------|
| postgres | checkbook-postgres-prod | 5432 (internal) | PostgreSQL 17 database |
| redis | checkbook-redis-prod | 6379 (internal) | Redis cache & rate limiting |
| api | checkbook-api-prod | 8080 (internal) | Spring Boot backend |
| ui | checkbook-ui-prod | 80 (internal) | React frontend with nginx |
| nginx | checkbook-nginx-prod | 80, 443 | Reverse proxy with SSL |
| certbot | checkbook-certbot | - | SSL certificate renewal (optional) |

## 🔑 Environment Variables

### Required Variables (`.env.prod`)

```bash
# Database
POSTGRES_DB=checkbook_prod
POSTGRES_USER=checkbook_user
POSTGRES_PASSWORD=<STRONG_PASSWORD>

# Redis
REDIS_PASSWORD=<STRONG_PASSWORD>

# JWT
JWT_SECRET=<64_CHAR_SECRET>
JWT_EXPIRATION=86400000

# Application URLs
APP_DOMAIN=checkbook.local-stokesnet.net
APP_BASE_URL=https://checkbook.local-stokesnet.net
VITE_API_BASE_URL=https://checkbook.local-stokesnet.net/api

# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<EMAIL>
MAIL_PASSWORD=<APP_PASSWORD>
MAIL_FROM=noreply@checkbook.local-stokesnet.net
```

### Generate Secure Values

```bash
# Database/Redis passwords
openssl rand -base64 32

# JWT Secret
openssl rand -base64 64
```

## 🚀 Deployment Commands

### Initial Deployment

```bash
# 1. Set up SSL certificates
./scripts/ssl/setup-mkcert.sh

# 2. Configure environment
cp .env.prod.example .env.prod
nano .env.prod  # Update with secure values

# 3. Deploy
./scripts/deploy/deploy.sh
```

### Maintenance Commands

```bash
# View logs
docker compose -f docker-compose.prod.yml logs -f

# Restart services
docker compose -f docker-compose.prod.yml restart

# Stop services
docker compose -f docker-compose.prod.yml down

# Backup database
./scripts/backup/backup-database.sh

# Check service status
docker compose -f docker-compose.prod.yml ps
```

## 🔒 Security Features

- ✅ HTTPS/TLS encryption with mkcert
- ✅ Rate limiting on API endpoints
- ✅ Strict CSP and security headers
- ✅ Non-root containers
- ✅ Password-protected Redis
- ✅ JWT authentication
- ✅ Request logging
- ✅ Health checks for all services

## 📊 Monitoring & Logs

### Log Locations

- **Nginx Access**: `nginx/logs/access.log`
- **Nginx Error**: `nginx/logs/error.log`
- **API Logs**: `checkbook-api/logs/`
- **Container Logs**: `docker compose -f docker-compose.prod.yml logs`

### Health Check Endpoints

- **Application**: `https://checkbook.local-stokesnet.net/health`
- **API**: `https://checkbook.local-stokesnet.net/api/health`
- **Nginx**: `http://localhost/health`

## 🔄 Backup Strategy

### Automated Backups

Set up cron job for daily backups:

```bash
crontab -e

# Add this line for daily backups at 2 AM
0 2 * * * /path/to/checkbook/scripts/backup/backup-database.sh >> /path/to/checkbook/logs/backup.log 2>&1
```

### Manual Backup

```bash
./scripts/backup/backup-database.sh
```

Backups are stored in `backups/` directory and retained for 30 days (configurable via `BACKUP_RETENTION_DAYS`).

## 🛠️ Troubleshooting

### Common Issues

1. **Certificate not trusted**
   - Ensure mkcert CA is installed on client device: `mkcert -install`
   - Copy CA from server if generated there

2. **Services won't start**
   - Check logs: `docker compose -f docker-compose.prod.yml logs`
   - Verify .env.prod has all required variables
   - Ensure ports 80 and 443 are available

3. **Can't access from network**
   - Verify firewall allows ports 80/443
   - Check /etc/hosts on client devices
   - Ensure server IP is correct

4. **Database connection errors**
   - Verify PostgreSQL is healthy: `docker ps`
   - Check credentials in .env.prod
   - View database logs: `docker logs checkbook-postgres-prod`

## 📚 Documentation

- [DEPLOYMENT.md](./DEPLOYMENT.md) - Complete deployment guide with troubleshooting
- [QUICK_START.md](./QUICK_START.md) - Quick reference for common tasks
- [README.md](./README.md) - Application overview and development guide

## 🔐 Security Checklist

Before deploying to production:

- [ ] Generate strong passwords for all services
- [ ] Configure JWT secret (64+ characters)
- [ ] Set up real SMTP server for email
- [ ] Configure firewall (UFW/iptables)
- [ ] Set up automated database backups
- [ ] Review nginx security headers
- [ ] Enable Docker security features
- [ ] Test backup/restore procedure
- [ ] Update all container images to latest stable
- [ ] Configure monitoring/alerting

## 📞 Support

For deployment issues:
1. Check container logs
2. Review DEPLOYMENT.md troubleshooting section
3. Verify environment configuration
4. Check network/firewall settings
