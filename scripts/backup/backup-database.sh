#!/bin/bash
# Database Backup Script for Checkbook Application

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/backups"
ENV_FILE="${PROJECT_ROOT}/.env.prod"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="checkbook_backup_${TIMESTAMP}.sql"

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}Database Backup Script${NC}"
echo -e "${GREEN}==================================${NC}"
echo

# Load environment variables
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: .env.prod file not found!${NC}"
    exit 1
fi

set -a
source "$ENV_FILE"
set +a

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Perform backup
echo -e "${GREEN}Creating backup of ${POSTGRES_DB}...${NC}"
docker exec checkbook-postgres-prod pg_dump \
    -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" \
    --clean \
    --if-exists \
    --create \
    > "${BACKUP_DIR}/${BACKUP_FILE}"

# Compress backup
echo -e "${GREEN}Compressing backup...${NC}"
gzip "${BACKUP_DIR}/${BACKUP_FILE}"

COMPRESSED_FILE="${BACKUP_FILE}.gz"
BACKUP_SIZE=$(du -h "${BACKUP_DIR}/${COMPRESSED_FILE}" | cut -f1)

echo
echo -e "${GREEN}✓ Backup completed successfully!${NC}"
echo -e "File: ${YELLOW}${BACKUP_DIR}/${COMPRESSED_FILE}${NC}"
echo -e "Size: ${YELLOW}${BACKUP_SIZE}${NC}"

# Clean old backups (keep last 30 days)
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-30}
echo
echo -e "${GREEN}Cleaning backups older than ${RETENTION_DAYS} days...${NC}"
find "$BACKUP_DIR" -name "checkbook_backup_*.sql.gz" -type f -mtime +${RETENTION_DAYS} -delete
echo -e "${GREEN}✓ Cleanup complete${NC}"