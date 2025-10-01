#!/bin/bash
# Checkbook Application Deployment Script
# This script handles the complete deployment process

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.prod"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.prod.yml"

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Checkbook Deployment Script${NC}"
echo -e "${GREEN}==================================${NC}"
echo

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   echo -e "${RED}Error: This script should not be run as root${NC}"
   exit 1
fi

# Check if .env.prod exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: .env.prod file not found!${NC}"
    echo -e "${YELLOW}Please copy .env.prod.example to .env.prod and configure it${NC}"
    exit 1
fi

# Load environment variables
set -a
source "$ENV_FILE"
set +a

# Check required environment variables
required_vars=("POSTGRES_PASSWORD" "JWT_SECRET" "APP_BASE_URL" "MAIL_HOST")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo -e "${RED}Error: Required environment variable $var is not set${NC}"
        exit 1
    fi
done

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

echo -e "${GREEN}Pre-deployment checks passed${NC}"
echo

# Ask for confirmation
read -p "$(echo -e ${YELLOW}Are you sure you want to deploy to production? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled"
    exit 0
fi

# Create necessary directories
echo -e "${GREEN}Creating necessary directories...${NC}"
mkdir -p "${PROJECT_ROOT}/backups"
mkdir -p "${PROJECT_ROOT}/nginx/logs"
mkdir -p "${PROJECT_ROOT}/nginx/ssl"
mkdir -p "${PROJECT_ROOT}/checkbook-api/logs"

# Pull latest code (if using git)
if [ -d "${PROJECT_ROOT}/.git" ]; then
    echo -e "${GREEN}Pulling latest code from repository...${NC}"
    cd "$PROJECT_ROOT"
    git pull || echo -e "${YELLOW}Warning: Git pull failed or not configured${NC}"
fi

# Build Docker images
echo -e "${GREEN}Building Docker images...${NC}"
cd "$PROJECT_ROOT"
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache

# Stop existing containers
echo -e "${GREEN}Stopping existing containers...${NC}"
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down || true

# Start containers
echo -e "${GREEN}Starting containers...${NC}"
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

# Wait for services to be healthy
echo -e "${GREEN}Waiting for services to be healthy...${NC}"
max_wait=120
waited=0
while [ $waited -lt $max_wait ]; do
    if docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps | grep -q "unhealthy"; then
        echo -e "${YELLOW}Waiting for services to become healthy... (${waited}s/${max_wait}s)${NC}"
        sleep 5
        waited=$((waited + 5))
    else
        break
    fi
done

# Check if all services are running
echo -e "${GREEN}Checking service status...${NC}"
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

# Test API health
echo -e "${GREEN}Testing API health endpoint...${NC}"
sleep 10
if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ API is healthy${NC}"
else
    echo -e "${YELLOW}Warning: API health check failed${NC}"
fi

# Show logs
echo
echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}==================================${NC}"
echo
echo -e "View logs with: ${YELLOW}docker-compose -f docker-compose.prod.yml logs -f${NC}"
echo -e "Stop services: ${YELLOW}docker-compose -f docker-compose.prod.yml down${NC}"
echo -e "Restart services: ${YELLOW}docker-compose -f docker-compose.prod.yml restart${NC}"
echo

# Display running containers
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps