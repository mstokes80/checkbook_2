#!/bin/bash
# mkcert SSL Certificate Setup Script for Checkbook Application
# Generates LAN-trusted certificates for local network deployment

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SSL_DIR="${PROJECT_ROOT}/nginx/ssl"
DOMAIN="checkbook.local-stokesnet.net"

echo -e "${GREEN}==================================${NC}"
echo -e "${GREEN}mkcert SSL Certificate Setup${NC}"
echo -e "${GREEN}==================================${NC}"
echo

# Check if mkcert is installed
if ! command -v mkcert &> /dev/null; then
    echo -e "${RED}Error: mkcert is not installed${NC}"
    echo
    echo -e "${YELLOW}Install mkcert:${NC}"
    echo "  macOS:   brew install mkcert"
    echo "  Linux:   See https://github.com/FiloSottile/mkcert#installation"
    echo
    exit 1
fi

# Create SSL directory
mkdir -p "$SSL_DIR"

# Check if CA is already installed
if ! mkcert -CAROOT &> /dev/null; then
    echo -e "${YELLOW}Note: Local CA not yet installed in system trust store${NC}"
    echo -e "${YELLOW}Run 'mkcert -install' manually to trust certificates on this device${NC}"
    echo
fi

# Generate certificate for the domain
echo -e "${GREEN}Generating certificate for ${DOMAIN}...${NC}"
cd "$SSL_DIR"

# Generate cert and key
mkcert -cert-file cert.pem -key-file key.pem "$DOMAIN" "*.local-stokesnet.net" localhost 127.0.0.1 ::1

# Set proper permissions
chmod 600 key.pem
chmod 644 cert.pem

echo
echo -e "${GREEN}✓ Certificate generated successfully!${NC}"
echo -e "${GREEN}==================================${NC}"
echo
echo -e "Domain: ${YELLOW}${DOMAIN}${NC}"
echo -e "Certificate: ${YELLOW}${SSL_DIR}/cert.pem${NC}"
echo -e "Private Key: ${YELLOW}${SSL_DIR}/key.pem${NC}"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo -e "1. Update your /etc/hosts file to point ${DOMAIN} to your server IP"
echo -e "   Example: echo '192.168.1.100 ${DOMAIN}' | sudo tee -a /etc/hosts"
echo -e "2. Deploy the application with: ${GREEN}./scripts/deploy/deploy.sh${NC}"
echo -e "3. Access your application at: ${GREEN}https://${DOMAIN}${NC}"
echo