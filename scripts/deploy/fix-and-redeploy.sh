#!/bin/bash
# Quick fix and redeploy script for Checkbook Application

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.prod"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.prod.yml"

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Checkbook Fix & Redeploy${NC}"
echo -e "${GREEN}==================================${NC}"
echo

# Check if .env.prod exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: .env.prod file not found!${NC}"
    exit 1
fi

cd "$PROJECT_ROOT"

# Stop existing containers
echo -e "${GREEN}Stopping existing containers...${NC}"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down

# Remove old containers and images (optional)
read -p "$(echo -e ${YELLOW}Do you want to remove old images and rebuild? [y/N]: ${NC})" -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${GREEN}Removing old images...${NC}"
    docker rmi checkbook-api:latest checkbook-ui:latest 2>/dev/null || true

    echo -e "${GREEN}Building new images...${NC}"
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache
fi

# Start containers with explicit env file
echo -e "${GREEN}Starting containers with .env.prod...${NC}"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

# Wait for services
echo -e "${GREEN}Waiting for services to start...${NC}"
sleep 10

# Check status
echo -e "${GREEN}Checking service status...${NC}"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

# Show logs
echo
echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Recent logs:${NC}"
echo -e "${GREEN}==================================${NC}"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs --tail=50

echo
echo -e "${GREEN}Deployment complete!${NC}"
echo -e "View logs: ${YELLOW}docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f${NC}"