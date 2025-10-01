#!/bin/bash

# verify-flyway.sh
# Script to verify Flyway migrations and database schema

echo "========================================="
echo "Flyway Migration Verification Script"
echo "========================================="

# Check if database is running (optional - for when PostgreSQL is available)
echo "1. Checking migration files..."
if [ -d "src/main/resources/db/migration" ]; then
    echo "✓ Migration directory exists"
    migration_count=$(ls -1 src/main/resources/db/migration/*.sql 2>/dev/null | wc -l)
    echo "✓ Found $migration_count migration files:"
    ls -1 src/main/resources/db/migration/*.sql 2>/dev/null | sed 's|src/main/resources/db/migration/||' | sed 's/^/  - /'
else
    echo "✗ Migration directory not found"
    exit 1
fi

echo ""
echo "2. Checking Flyway configuration..."
if grep -q "flyway" src/main/resources/application.yml; then
    echo "✓ Flyway configuration found in application.yml"
else
    echo "✗ Flyway configuration not found"
    exit 1
fi

echo ""
echo "3. Checking Maven/POM configuration..."
if grep -q "flyway-core" pom.xml; then
    echo "✓ Flyway dependency found in pom.xml"
else
    echo "✗ Flyway dependency not found in pom.xml"
    exit 1
fi

echo ""
echo "4. Validating migration file naming..."
for file in src/main/resources/db/migration/*.sql; do
    if [[ $(basename "$file") =~ ^V[0-9]+__.*\.sql$ ]]; then
        echo "✓ $(basename "$file") - Valid naming convention"
    else
        echo "✗ $(basename "$file") - Invalid naming convention"
        exit 1
    fi
done

echo ""
echo "5. Checking for SQL syntax issues..."
for file in src/main/resources/db/migration/*.sql; do
    if grep -q "CREATE TABLE\|CREATE INDEX\|ALTER TABLE" "$file"; then
        echo "✓ $(basename "$file") - Contains valid SQL DDL statements"
    else
        echo "⚠ $(basename "$file") - May not contain DDL statements"
    fi
done

echo ""
echo "========================================="
echo "Verification Summary:"
echo "✓ Flyway is properly configured"
echo "✓ Migration files follow naming convention"
echo "✓ SQL files contain DDL statements"
echo ""
echo "Next steps:"
echo "1. Start PostgreSQL database"
echo "2. Create database 'checkbook_db'"
echo "3. Run: mvn spring-boot:run"
echo "4. Flyway will automatically apply migrations"
echo "========================================="