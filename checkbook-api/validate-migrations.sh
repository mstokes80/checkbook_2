#!/bin/bash

# validate-migrations.sh
# Comprehensive validation script for database migrations

set -e  # Exit on any error

echo "========================================="
echo "Database Migration Validation Script"
echo "========================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "success")
            echo -e "${GREEN}✓${NC} $message"
            ;;
        "error")
            echo -e "${RED}✗${NC} $message"
            ;;
        "warning")
            echo -e "${YELLOW}⚠${NC} $message"
            ;;
        "info")
            echo -e "ℹ $message"
            ;;
    esac
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_status "error" "pom.xml not found. Please run this script from the checkbook-api directory."
    exit 1
fi

echo "1. Project Structure Validation"
echo "==============================="

# Check Maven project structure
if [ -d "src/main/java" ]; then
    print_status "success" "Java source directory exists"
else
    print_status "error" "Java source directory missing"
    exit 1
fi

if [ -d "src/main/resources" ]; then
    print_status "success" "Resources directory exists"
else
    print_status "error" "Resources directory missing"
    exit 1
fi

if [ -d "src/main/resources/db/migration" ]; then
    print_status "success" "Migration directory exists"
else
    print_status "error" "Migration directory missing"
    exit 1
fi

echo ""
echo "2. Migration Files Validation"
echo "=============================="

# Count migration files
migration_count=$(find src/main/resources/db/migration -name "*.sql" | wc -l)
print_status "info" "Found $migration_count migration files"

# Validate each migration file
for file in src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")

        # Check naming convention
        if [[ $filename =~ ^V[0-9]+__.*\.sql$ ]]; then
            print_status "success" "$filename - Valid naming convention"
        else
            print_status "error" "$filename - Invalid naming convention"
            exit 1
        fi

        # Check file is not empty
        if [ -s "$file" ]; then
            print_status "success" "$filename - File is not empty"
        else
            print_status "error" "$filename - File is empty"
            exit 1
        fi

        # Check for SQL keywords
        if grep -qi "CREATE\|ALTER\|DROP\|INSERT" "$file"; then
            print_status "success" "$filename - Contains SQL statements"
        else
            print_status "warning" "$filename - May not contain SQL statements"
        fi

        # Check for potential issues
        if grep -qi "DROP TABLE IF EXISTS" "$file"; then
            print_status "warning" "$filename - Contains DROP TABLE statements (review carefully)"
        fi

        # Validate SQL syntax (basic check)
        if grep -qi "CREATE TABLE.*(" "$file" && grep -qi ");" "$file"; then
            print_status "success" "$filename - CREATE TABLE syntax appears valid"
        elif grep -qi "CREATE TABLE" "$file"; then
            print_status "warning" "$filename - CREATE TABLE syntax may be incomplete"
        fi
    fi
done

echo ""
echo "3. Configuration Validation"
echo "============================"

# Check application.yml
if [ -f "src/main/resources/application.yml" ]; then
    print_status "success" "application.yml exists"

    # Check for required configuration
    if grep -q "spring:" src/main/resources/application.yml; then
        print_status "success" "Spring configuration found"
    else
        print_status "error" "Spring configuration missing"
        exit 1
    fi

    if grep -q "datasource:" src/main/resources/application.yml; then
        print_status "success" "Datasource configuration found"
    else
        print_status "error" "Datasource configuration missing"
        exit 1
    fi

    if grep -q "flyway:" src/main/resources/application.yml; then
        print_status "success" "Flyway configuration found"
    else
        print_status "error" "Flyway configuration missing"
        exit 1
    fi

    if grep -q "jpa:" src/main/resources/application.yml; then
        print_status "success" "JPA configuration found"
    else
        print_status "error" "JPA configuration missing"
        exit 1
    fi

    # Check for security settings
    if grep -q "ddl-auto: validate" src/main/resources/application.yml; then
        print_status "success" "JPA DDL validation mode is correctly set to 'validate'"
    else
        print_status "warning" "JPA DDL mode should be 'validate' to let Flyway manage schema"
    fi

else
    print_status "error" "application.yml not found"
    exit 1
fi

echo ""
echo "4. Maven Dependencies Validation"
echo "================================="

# Check pom.xml for required dependencies
dependencies=(
    "spring-boot-starter-data-jpa"
    "spring-boot-starter-web"
    "spring-boot-starter-security"
    "postgresql"
    "flyway-core"
)

for dep in "${dependencies[@]}"; do
    if grep -q "$dep" pom.xml; then
        print_status "success" "$dep dependency found"
    else
        print_status "error" "$dep dependency missing"
        exit 1
    fi
done

echo ""
echo "5. SQL Syntax Deep Validation"
echo "=============================="

# More detailed SQL validation
for file in src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")

        # Check for common SQL issues
        if grep -q ";" "$file"; then
            print_status "success" "$filename - Contains statement terminators"
        else
            print_status "warning" "$filename - May be missing statement terminators"
        fi

        # Check for foreign key constraints
        if grep -qi "FOREIGN KEY\|REFERENCES" "$file"; then
            print_status "info" "$filename - Contains foreign key relationships"
        fi

        # Check for indexes
        if grep -qi "CREATE INDEX\|CREATE UNIQUE INDEX" "$file"; then
            print_status "info" "$filename - Contains index definitions"
        fi

        # Check for triggers
        if grep -qi "CREATE TRIGGER\|CREATE OR REPLACE FUNCTION" "$file"; then
            print_status "info" "$filename - Contains triggers or functions"
        fi

        # Check for comments
        if grep -qi "COMMENT ON" "$file"; then
            print_status "success" "$filename - Contains documentation comments"
        fi
    fi
done

echo ""
echo "6. Security Validation"
echo "======================"

# Check for potential security issues
security_issues=0

for file in src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")

        # Check for plain text passwords (should not be in migrations)
        if grep -i "password.*=" "$file" | grep -v "password_hash"; then
            print_status "warning" "$filename - May contain plain text passwords"
            ((security_issues++))
        fi

        # Check for proper password hashing setup
        if grep -qi "password_hash" "$file"; then
            print_status "success" "$filename - Uses password hashing field"
        fi
    fi
done

if [ $security_issues -eq 0 ]; then
    print_status "success" "No obvious security issues found in migrations"
fi

echo ""
echo "7. Maven Compilation Test"
echo "========================="

# Test Maven compilation
print_status "info" "Testing Maven compilation..."
if mvn compile -q; then
    print_status "success" "Maven compilation successful"
else
    print_status "error" "Maven compilation failed"
    exit 1
fi

echo ""
echo "8. Migration Order Validation"
echo "=============================="

# Verify migration files are in correct order
migration_files=($(ls src/main/resources/db/migration/V*.sql | sort -V))
prev_version=""

for file in "${migration_files[@]}"; do
    filename=$(basename "$file")
    version=$(echo "$filename" | sed 's/V\([0-9]*\)__.*/\1/')

    if [ -n "$prev_version" ] && [ "$version" -le "$prev_version" ]; then
        print_status "error" "Migration version conflict: $filename (version $version should be > $prev_version)"
        exit 1
    fi

    print_status "success" "$filename - Version $version is in correct order"
    prev_version=$version
done

echo ""
echo "========================================="
echo "Validation Summary"
echo "========================================="

print_status "success" "All validations passed!"
print_status "info" "Migration files are ready for deployment"
print_status "info" "Found $migration_count migration files in correct order"

echo ""
echo "Next Steps:"
echo "1. Start PostgreSQL database server"
echo "2. Create database 'checkbook_db' and user 'checkbook_user'"
echo "3. Run: mvn spring-boot:run"
echo "4. Verify migrations applied: mvn flyway:info"
echo ""
echo "For database setup instructions, see DATABASE_SETUP.md"
echo "For rollback procedures, see rollback-procedures.sql"
echo "========================================="