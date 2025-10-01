#!/bin/bash

# Migration Validation Script for Transaction Recording Feature
# This script validates database migrations in a production-like environment

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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if psql is installed
    if ! command -v psql &> /dev/null; then
        log_error "psql is not installed. Please install PostgreSQL client."
        exit 1
    fi

    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed. Please install Maven."
        exit 1
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

# Backup current database schema
backup_schema() {
    log_info "Creating schema backup..."

    BACKUP_FILE="schema_backup_$(date +%Y%m%d_%H%M%S).sql"
    PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME --schema-only > $BACKUP_FILE

    if [ $? -eq 0 ]; then
        log_info "Schema backup created: $BACKUP_FILE"
    else
        log_error "Failed to create schema backup."
        exit 1
    fi
}

# Validate migration files
validate_migration_files() {
    log_info "Validating migration files..."

    # Check if migration files exist
    if [ ! -f "src/main/resources/db/migration/V10__create_transaction_categories.sql" ]; then
        log_error "V10__create_transaction_categories.sql not found"
        exit 1
    fi

    if [ ! -f "src/main/resources/db/migration/V11__create_transactions.sql" ]; then
        log_error "V11__create_transactions.sql not found"
        exit 1
    fi

    log_info "Migration files validation completed."
}

# Run Flyway migration
run_migration() {
    log_info "Running Flyway migration..."

    mvn flyway:migrate \
        -Dflyway.url=jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME \
        -Dflyway.user=$DB_USER \
        -Dflyway.password=$DB_PASSWORD

    if [ $? -eq 0 ]; then
        log_info "Migration completed successfully."
    else
        log_error "Migration failed."
        exit 1
    fi
}

# Validate migration results
validate_migration_results() {
    log_info "Validating migration results..."

    # Check if tables were created
    TABLES_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_name IN ('transaction_categories', 'transactions');
    " | tr -d ' ')

    if [ "$TABLES_COUNT" != "2" ]; then
        log_error "Expected 2 tables (transaction_categories, transactions), found $TABLES_COUNT"
        exit 1
    fi

    # Check if default categories were inserted
    CATEGORIES_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM transaction_categories WHERE is_system_default = true;
    " | tr -d ' ')

    if [ "$CATEGORIES_COUNT" != "13" ]; then
        log_error "Expected 13 default categories, found $CATEGORIES_COUNT"
        exit 1
    fi

    # Check if indexes were created
    INDEXES_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
        SELECT COUNT(*) FROM pg_indexes
        WHERE tablename IN ('transaction_categories', 'transactions');
    " | tr -d ' ')

    if [ "$INDEXES_COUNT" -lt "10" ]; then
        log_warn "Expected at least 10 indexes, found $INDEXES_COUNT"
    fi

    log_info "Migration validation completed successfully."
}

# Performance test
run_performance_test() {
    log_info "Running basic performance test..."

    # Insert test transactions
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
        INSERT INTO transactions (account_id, amount, description, transaction_date, running_balance, created_by, updated_by)
        SELECT
            1 as account_id,
            (RANDOM() * 1000 - 500)::DECIMAL(12,2) as amount,
            'Test transaction ' || generate_series as description,
            CURRENT_DATE - (RANDOM() * 365)::INTEGER as transaction_date,
            0 as running_balance,
            'test_user' as created_by,
            'test_user' as updated_by
        FROM generate_series(1, 1000);
    " > /dev/null 2>&1

    if [ $? -eq 0 ]; then
        log_info "Performance test data inserted successfully."

        # Clean up test data
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
            DELETE FROM transactions WHERE created_by = 'test_user';
        " > /dev/null 2>&1

        log_info "Performance test completed and test data cleaned up."
    else
        log_warn "Performance test failed."
    fi
}

# Main execution
main() {
    log_info "Starting migration validation process..."

    check_prerequisites
    test_db_connection
    validate_migration_files
    backup_schema
    run_migration
    validate_migration_results
    run_performance_test

    log_info "Migration validation completed successfully!"
    log_info "The transaction recording feature is ready for production deployment."
}

# Execute main function
main "$@"