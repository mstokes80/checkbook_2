# Troubleshooting Guide - Checkbook Deployment Issues

## Issue 1: Environment Variables Not Loading

### Symptoms
```
WARN[0000] The "REDIS_PASSWORD" variable is not set. Defaulting to a blank string.
WARN[0000] The "MAIL_USERNAME" variable is not set. Defaulting to a blank string.
```

### Root Cause
Docker Compose doesn't automatically load `.env.prod` - it only loads `.env` by default.

### Solution

**Always use `--env-file` flag:**

```bash
# CORRECT ✅
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# WRONG ❌
docker compose -f docker-compose.prod.yml up -d
```

### Quick Fix

Run the fix script:
```bash
./scripts/deploy/fix-and-redeploy.sh
```

Or manually:
```bash
# Stop services
docker compose -f docker-compose.prod.yml --env-file .env.prod down

# Restart with env file
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### Verify Environment Variables Are Loaded

```bash
# Check if environment variables are set in containers
docker exec checkbook-api-prod env | grep POSTGRES_PASSWORD
docker exec checkbook-redis-prod env | grep REDIS
```

---

## Issue 2: Nginx Showing Error Page Instead of Application

### Symptoms
- Browser shows nginx error page (502 Bad Gateway, 404, etc.)
- Application UI not loading
- Default nginx error pages instead of React app

### Root Cause
Nginx upstream configuration was pointing to wrong service (API backend was pointing to UI).

### Solution

The nginx configuration has been fixed. The correct upstream configuration is:

```nginx
# Upstream backend API
upstream api_backend {
    server api:8080;  # ✅ Correct
    keepalive 32;
}

# Upstream frontend UI
upstream ui_backend {
    server ui:80;     # ✅ Correct
    keepalive 32;
}
```

### Verify Fix

1. **Check nginx configuration:**
```bash
docker exec checkbook-nginx-prod nginx -t
```

2. **Check if services are reachable:**
```bash
# Test API from nginx container
docker exec checkbook-nginx-prod wget -q -O- http://api:8080/api/health

# Test UI from nginx container
docker exec checkbook-nginx-prod wget -q -O- http://ui:80
```

3. **Restart nginx:**
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod restart nginx
```

---

## Issue 3: Services Not Starting

### Check Service Status
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

### View Logs
```bash
# All services
docker compose -f docker-compose.prod.yml --env-file .env.prod logs

# Specific service
docker compose -f docker-compose.prod.yml --env-file .env.prod logs api
docker compose -f docker-compose.prod.yml --env-file .env.prod logs ui
docker compose -f docker-compose.prod.yml --env-file .env.prod logs nginx
```

### Common Issues

#### PostgreSQL Won't Start
```bash
# Check PostgreSQL logs
docker logs checkbook-postgres-prod

# Common issue: port 5432 already in use
sudo lsof -i :5432
sudo systemctl stop postgresql  # If system PostgreSQL is running
```

#### Redis Won't Start
```bash
# Check Redis logs
docker logs checkbook-redis-prod

# Verify password is set
docker exec checkbook-redis-prod redis-cli -a <YOUR_REDIS_PASSWORD> ping
```

#### API Won't Start
```bash
# Check API logs
docker logs checkbook-api-prod --tail 100

# Common issues:
# - Database connection failed → Check PostgreSQL is healthy
# - Port 8080 in use → Change port mapping or stop conflicting service
```

---

## Issue 4: Can't Access Application from Browser

### Check Firewall
```bash
# Ubuntu UFW
sudo ufw status
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# iptables
sudo iptables -L -n | grep -E "80|443"
```

### Check Hosts File
On client device:
```bash
# Linux/Mac
cat /etc/hosts | grep checkbook

# Should show:
<SERVER_IP> checkbook.local-stokesnet.net
```

### Test Connection
```bash
# From server
curl -k https://localhost

# From client
curl -k https://checkbook.local-stokesnet.net
ping checkbook.local-stokesnet.net
```

---

## Issue 5: SSL Certificate Errors

### Verify Certificate
```bash
# Check certificate validity
openssl x509 -in nginx/ssl/cert.pem -text -noout | grep -A 2 "Validity"

# Check certificate matches domain
openssl x509 -in nginx/ssl/cert.pem -text -noout | grep "DNS:"
```

### Trust Certificate on Client
```bash
# macOS
mkcert -install

# Linux
mkcert -install

# Copy CA from server if needed
scp USER@SERVER:/home/USER/.local/share/mkcert/rootCA.pem .
```

---

## Issue 6: Database Connection Errors

### Test Database Connection
```bash
# From host
docker exec -it checkbook-postgres-prod psql -U checkbook_user -d checkbook_prod

# From API container
docker exec -it checkbook-api-prod curl postgres:5432
```

### Reset Database
```bash
# ⚠️  WARNING: This deletes all data!
docker compose -f docker-compose.prod.yml --env-file .env.prod down -v
docker volume rm checkbook-prod_postgres_prod_data
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

---

## Complete Redeploy Process

If you need to start fresh:

```bash
# 1. Stop everything
docker compose -f docker-compose.prod.yml --env-file .env.prod down

# 2. Remove volumes (⚠️  deletes data)
docker volume rm checkbook-prod_postgres_prod_data checkbook-prod_redis_prod_data

# 3. Remove images
docker rmi checkbook-api:latest checkbook-ui:latest

# 4. Rebuild and start
docker compose -f docker-compose.prod.yml --env-file .env.prod build --no-cache
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 5. Check logs
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f
```

---

## Useful Commands Reference

### Start/Stop Services
```bash
# Start
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Stop
docker compose -f docker-compose.prod.yml --env-file .env.prod down

# Restart
docker compose -f docker-compose.prod.yml --env-file .env.prod restart

# Restart specific service
docker compose -f docker-compose.prod.yml --env-file .env.prod restart nginx
```

### View Logs
```bash
# Follow all logs
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f

# Last 100 lines
docker compose -f docker-compose.prod.yml --env-file .env.prod logs --tail=100

# Specific service
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f api
```

### Check Status
```bash
# Service status
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

# Individual container
docker ps | grep checkbook

# Container health
docker inspect --format='{{.State.Health.Status}}' checkbook-api-prod
```

### Execute Commands in Containers
```bash
# API container
docker exec -it checkbook-api-prod sh

# Database
docker exec -it checkbook-postgres-prod psql -U checkbook_user -d checkbook_prod

# Redis
docker exec -it checkbook-redis-prod redis-cli -a <PASSWORD>

# Nginx
docker exec -it checkbook-nginx-prod sh
```

---

## Getting Help

1. **Check logs first** - Most issues show up in logs
2. **Verify environment variables** - Ensure `.env.prod` is loaded
3. **Test connectivity** - Check firewall, hosts file, network
4. **Review this troubleshooting guide**
5. **Check individual service health**

### Collect Debug Information
```bash
# Create debug report
cat > debug-report.txt << EOF
=== Service Status ===
$(docker compose -f docker-compose.prod.yml --env-file .env.prod ps)

=== Docker Info ===
$(docker version)
$(docker compose version)

=== System Info ===
$(uname -a)

=== Firewall Status ===
$(sudo ufw status 2>/dev/null || echo "UFW not available")

=== Recent Logs ===
$(docker compose -f docker-compose.prod.yml --env-file .env.prod logs --tail=50)
EOF

cat debug-report.txt
```