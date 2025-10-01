#!/bin/bash
# Environment Variable Validation Script
# This script helps diagnose .env.prod file issues

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.prod"

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Environment Variable Validator${NC}"
echo -e "${GREEN}==================================${NC}"
echo

# Check if .env.prod exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: .env.prod file not found!${NC}"
    exit 1
fi

echo -e "${GREEN}Checking .env.prod file...${NC}"
echo

# Try to source the file
echo -e "${YELLOW}Loading environment variables...${NC}"
set -a
if source "$ENV_FILE" 2>&1 | tee /tmp/env_errors.txt; then
    echo -e "${GREEN}✓ File loaded successfully${NC}"
else
    echo -e "${RED}✗ Error loading file${NC}"
    cat /tmp/env_errors.txt
    exit 1
fi
set +a

echo

# Check critical variables
echo -e "${YELLOW}Checking critical variables:${NC}"
critical_vars=(
    "POSTGRES_DB"
    "POSTGRES_USER"
    "POSTGRES_PASSWORD"
    "REDIS_PASSWORD"
    "JWT_SECRET"
    "APP_BASE_URL"
    "VITE_API_BASE_URL"
    "MAIL_HOST"
    "MAIL_PORT"
    "MAIL_USERNAME"
    "MAIL_PASSWORD"
    "MAIL_FROM"
)

all_set=true
for var in "${critical_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo -e "${RED}✗ $var is NOT set${NC}"
        all_set=false
    else
        # Show first 10 chars only for security
        value="${!var}"
        if [ ${#value} -gt 10 ]; then
            display="${value:0:10}..."
        else
            display="${value}"
        fi
        echo -e "${GREEN}✓ $var = ${display}${NC}"
    fi
done

echo

if [ "$all_set" = true ]; then
    echo -e "${GREEN}All critical variables are set!${NC}"
    echo
    echo -e "${YELLOW}Testing Docker Compose variable loading...${NC}"

    # Test if docker compose can read the file
    cd "$PROJECT_ROOT"
    if docker compose -f docker-compose.prod.yml --env-file .env.prod config > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Docker Compose can read .env.prod${NC}"
    else
        echo -e "${RED}✗ Docker Compose cannot read .env.prod${NC}"
        echo -e "${YELLOW}Running verbose check...${NC}"
        docker compose -f docker-compose.prod.yml --env-file .env.prod config 2>&1 | head -20
    fi
else
    echo -e "${RED}Some variables are missing. Please fix .env.prod${NC}"
    exit 1
fi

echo
echo -e "${GREEN}Validation complete!${NC}"