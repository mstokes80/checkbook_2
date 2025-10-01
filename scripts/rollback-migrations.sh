#!/bin/bash

# Rollback Script for Transaction Recording Feature
# This script provides procedures to rollback database migrations and application deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-checkbook}
DB_USER=${DB_USER:-checkbook_user}
DB_PASSWORD=${DB_PASSWORD:-}

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Confirmation prompt
confirm_action() {
    local message="$1"
    echo -e "${YELLOW}$message${NC}"
    read -p "Are you sure you want to continue? (yes/no): " response
    if [[ "$response" != "yes" ]]; then
        log_info "Operation cancelled by user."
        exit 0
    fi
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if psql is installed
    if ! command -v psql &> /dev/null; then
        log_error "psql is not installed. Please install PostgreSQL client."
        exit 1
    fi

    # Check if kubectl is installed (for application rollback)
    if ! command -v kubectl &> /dev/null; then
        log_warn "kubectl is not installed. Application rollback will be manual."
    fi

    log_info "Prerequisites check completed."
}

# Test database connection
test_db_connection() {
    log_info "Testing database connection..."

    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1;" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        log_info "Database connection successful."
    else
        log_error "Failed to connect to database. Please check connection parameters."
        exit 1
    fi
}

# Create pre-rollback backup
create_backup() {
    log_info "Creating pre-rollback backup..."

    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="pre_rollback_backup_${timestamp}.sql"

    PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME > $backup_file

    if [ $? -eq 0 ]; then
        log_info "Pre-rollback backup created: $backup_file"
        echo $backup_file
    else
        log_error "Failed to create pre-rollback backup."
        exit 1
    fi
}

# Check if transaction data exists
check_transaction_data() {
    log_info "Checking for existing transaction data..."

    local transaction_count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'transactions';
    " 2>/dev/null | tr -d ' ' | head -1)

    if [ "$transaction_count" == "1" ]; then
        local data_count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
            SELECT COUNT(*) FROM transactions;
        " 2>/dev/null | tr -d ' ' | head -1)

        if [ "$data_count" -gt "0" ]; then
            log_warn "Found $data_count transactions in the database."
            log_warn "Rolling back will permanently delete all transaction data!"
            confirm_action "This will permanently delete $data_count transactions!"
        fi
    fi
}

# Rollback database schema
rollback_database() {
    log_info "Rolling back database schema..."

    confirm_action "This will drop the transaction tables and all related data!"

    # Drop tables in correct order (reverse of creation)
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << EOF
-- Drop transactions table (depends on transaction_categories)
DROP TABLE IF EXISTS transactions CASCADE;

-- Drop transaction_categories table
DROP TABLE IF EXISTS transaction_categories CASCADE;

-- Update flyway schema history to remove transaction migrations
DELETE FROM flyway_schema_history WHERE version IN ('10', '11');

-- Verify tables are dropped
\dt transaction*

EOF

    if [ $? -eq 0 ]; then
        log_info "Database rollback completed successfully."
    else
        log_error "Database rollback failed."
        exit 1
    fi
}

# Rollback application (Kubernetes)
rollback_application_k8s() {
    log_info "Rolling back application deployment (Kubernetes)..."

    if command -v kubectl &> /dev/null; then
        # Rollback API deployment
        kubectl rollout undo deployment/checkbook-api
        if [ $? -eq 0 ]; then
            log_info "API deployment rollback initiated."
        else
            log_error "Failed to rollback API deployment."
        fi

        # Rollback UI deployment
        kubectl rollout undo deployment/checkbook-ui
        if [ $? -eq 0 ]; then
            log_info "UI deployment rollback initiated."
        else
            log_error "Failed to rollback UI deployment."
        fi

        # Wait for rollback to complete
        log_info "Waiting for rollback to complete..."
        kubectl rollout status deployment/checkbook-api --timeout=300s
        kubectl rollout status deployment/checkbook-ui --timeout=300s

        log_info "Application rollback completed."
    else
        log_warn "kubectl not available. Please manually rollback application deployments."
    fi
}

# Rollback application (Docker Compose)
rollback_application_docker() {
    log_info "Rolling back application deployment (Docker)..."

    if [ -f "docker-compose.yml" ]; then
        # Stop current containers
        docker-compose down

        # Rollback to previous image tags (assuming you have previous tags)
        log_warn "Please manually update docker-compose.yml to use previous image tags."
        log_warn "Then run: docker-compose up -d"
    else
        log_warn "docker-compose.yml not found. Please manually rollback application."
    fi
}

# Validate rollback
validate_rollback() {
    log_info "Validating rollback..."

    # Check that transaction tables are removed
    local tables_count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_name IN ('transaction_categories', 'transactions');
    " | tr -d ' ')

    if [ "$tables_count" == "0" ]; then
        log_info "Transaction tables successfully removed."
    else
        log_error "Transaction tables still exist. Rollback may have failed."
        exit 1
    fi

    # Check flyway schema history
    local flyway_count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('10', '11');
    " | tr -d ' ')

    if [ "$flyway_count" == "0" ]; then
        log_info "Flyway schema history cleaned up successfully."
    else
        log_warn "Flyway schema history may still contain transaction migration entries."
    fi

    log_info "Rollback validation completed."
}

# Display rollback options
show_menu() {
    echo ""
    echo "Transaction Recording Feature Rollback Options:"
    echo "1) Full rollback (database + application)"
    echo "2) Database only rollback"
    echo "3) Application only rollback (Kubernetes)"
    echo "4) Application only rollback (Docker)"
    echo "5) Validate current state"
    echo "6) Exit"
    echo ""
}

# Main execution
main() {
    log_info "Transaction Recording Feature Rollback Script"
    log_warn "This script will rollback the transaction recording feature deployment."

    check_prerequisites
    test_db_connection

    while true; do
        show_menu
        read -p "Please select an option (1-6): " choice

        case $choice in
            1)
                log_info "Performing full rollback..."
                backup_file=$(create_backup)
                check_transaction_data
                rollback_database
                rollback_application_k8s
                validate_rollback
                log_info "Full rollback completed. Backup saved as: $backup_file"
                break
                ;;
            2)
                log_info "Performing database-only rollback..."
                backup_file=$(create_backup)
                check_transaction_data
                rollback_database
                validate_rollback
                log_info "Database rollback completed. Backup saved as: $backup_file"
                break
                ;;
            3)
                log_info "Performing application-only rollback (Kubernetes)..."
                rollback_application_k8s
                log_info "Application rollback completed."
                break
                ;;
            4)
                log_info "Performing application-only rollback (Docker)..."
                rollback_application_docker
                log_info "Application rollback completed."
                break
                ;;
            5)
                validate_rollback
                ;;
            6)
                log_info "Exiting rollback script."
                exit 0
                ;;
            *)
                log_error "Invalid option. Please select 1-6."
                ;;
        esac
    done
}

# Execute main function
main "$@"