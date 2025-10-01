# Production Deployment Plan - Transaction Recording Feature

## Overview
This document outlines the production deployment plan for the Transaction Recording feature, including database migrations, rollback procedures, and monitoring setup.

## Database Migration Deployment Plan

### Migration Files
The following Flyway migrations need to be deployed:

1. **V10__create_transaction_categories.sql**
   - Creates `transaction_categories` table
   - Adds indexes for performance
   - Inserts 13 default system categories
   - Impact: Low risk, adds new table only

2. **V11__create_transactions.sql**
   - Creates `transactions` table with foreign keys to accounts and categories
   - Adds comprehensive indexes for query performance
   - Includes data validation constraints
   - Impact: Medium risk, large table with potential for high volume

### Pre-Deployment Checklist

#### Database Backup
```bash
# Create full database backup before deployment
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME > checkbook_backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup integrity
pg_restore --list checkbook_backup_$(date +%Y%m%d_%H%M%S).sql
```

#### Migration Validation
```bash
# Test migrations on staging environment first
mvn flyway:info -Dflyway.url=$STAGING_DB_URL
mvn flyway:migrate -Dflyway.url=$STAGING_DB_URL

# Validate migration status
mvn flyway:validate -Dflyway.url=$STAGING_DB_URL
```

#### Performance Testing
- Test transaction insertion performance with 10k+ records
- Validate query performance with complex filtering
- Monitor memory usage during bulk operations

### Deployment Steps

#### Phase 1: Database Schema Deployment
1. **Maintenance Window**: Schedule 30-minute maintenance window
2. **Application Shutdown**: Stop application servers to prevent data corruption
3. **Database Migration**:
   ```bash
   # Run migrations
   mvn flyway:migrate -Dflyway.url=$PROD_DB_URL

   # Verify migration success
   mvn flyway:info -Dflyway.url=$PROD_DB_URL
   ```
4. **Post-Migration Validation**:
   ```sql
   -- Verify tables created
   SELECT table_name FROM information_schema.tables
   WHERE table_schema = 'public'
   AND table_name IN ('transaction_categories', 'transactions');

   -- Verify default categories inserted
   SELECT COUNT(*) FROM transaction_categories WHERE is_system_default = true;
   -- Expected: 13 categories

   -- Verify indexes created
   SELECT indexname, tablename FROM pg_indexes
   WHERE tablename IN ('transaction_categories', 'transactions');
   ```

#### Phase 2: Application Deployment
1. **Deploy Application**: Deploy updated application with transaction features
2. **Smoke Tests**: Run basic functionality tests
3. **Load Testing**: Verify performance under normal load

### Rollback Procedures

#### Database Rollback
```sql
-- Rollback V11 (transactions table)
DROP TABLE IF EXISTS transactions CASCADE;

-- Rollback V10 (transaction_categories table)
DROP TABLE IF EXISTS transaction_categories CASCADE;

-- Update flyway schema history
DELETE FROM flyway_schema_history WHERE version IN ('10', '11');
```

#### Application Rollback
```bash
# Rollback to previous application version
kubectl rollout undo deployment/checkbook-api
kubectl rollout undo deployment/checkbook-ui

# Or using blue-green deployment
# Switch traffic back to previous version
```

### Risk Assessment

#### High Risk Areas
- **Large Dataset Migration**: If existing accounts have many transactions
- **Foreign Key Constraints**: Potential for constraint violations
- **Index Creation**: May take significant time on large datasets

#### Mitigation Strategies
- **Staged Deployment**: Deploy to subset of users first
- **Database Connection Pooling**: Ensure adequate connection limits
- **Monitoring**: Real-time monitoring during deployment

## Monitoring and Alerting

### Database Monitoring
```sql
-- Monitor migration progress
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 10;

-- Monitor table sizes
SELECT
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE tablename IN ('transactions', 'transaction_categories');

-- Monitor index usage
SELECT
    indexrelname,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE relname IN ('transactions', 'transaction_categories');
```

### Application Monitoring
- **Response Times**: Transaction CRUD operations < 200ms
- **Error Rates**: < 0.1% error rate for transaction operations
- **Throughput**: Monitor transactions per second

### Performance Dashboards

#### Key Metrics to Monitor
1. **Database Performance**
   - Query execution time
   - Connection pool usage
   - Lock wait times
   - Index hit ratios

2. **API Performance**
   - Transaction creation latency
   - Transaction list query performance
   - Running balance calculation time
   - Bulk operation performance

3. **Business Metrics**
   - Transactions created per hour
   - Active users using transaction features
   - Category usage distribution

### Alerting Rules
```yaml
# Example Prometheus alerting rules
groups:
  - name: transaction_alerts
    rules:
      - alert: TransactionAPIHighLatency
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{endpoint="/api/transactions"}[5m])) > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Transaction API latency is high"

      - alert: TransactionDatabaseConnectionHigh
        expr: pg_stat_activity_count{state="active"} > 80
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High number of active database connections"
```

## Post-Deployment Validation

### Functional Testing
```bash
# Test transaction CRUD operations
curl -X POST /api/transactions -d '{
  "accountId": 1,
  "amount": -50.00,
  "description": "Test transaction",
  "transactionDate": "2025-09-30"
}'

# Test transaction filtering
curl "/api/transactions?accountId=1&startDate=2025-01-01&endDate=2025-12-31"

# Test bulk categorization
curl -X POST /api/transactions/bulk-categorize -d '{
  "transactionIds": [1, 2, 3],
  "categoryId": 1
}'
```

### Performance Validation
- Transaction creation: < 100ms
- Transaction list (20 items): < 150ms
- Running balance calculation: < 50ms
- Bulk operations (100 items): < 500ms

## Success Criteria

### Technical Success
- ✅ All migrations applied successfully
- ✅ No data corruption or loss
- ✅ All tests passing
- ✅ Performance metrics within targets
- ✅ No critical errors in logs

### Business Success
- ✅ Users can create, edit, and delete transactions
- ✅ Running balances calculate correctly
- ✅ Categorization works as expected
- ✅ Filtering and search perform well
- ✅ No user-facing errors

## Emergency Contacts
- **Database Team**: dba-oncall@company.com
- **DevOps Team**: devops-oncall@company.com
- **Development Team**: dev-team@company.com

## Documentation Links
- [API Documentation](./api-documentation.md)
- [Database Schema](./database-schema.md)
- [Monitoring Setup Guide](../monitoring/monitoring-setup-guide.md)
- [Transaction Performance Dashboard](../monitoring/grafana/transaction-performance-dashboard.json)
- [Prometheus Alert Rules](../monitoring/prometheus/transaction-alerts.yml)

## Monitoring Configuration Files

### Deployment Validation Scripts
- **Migration Validation**: `scripts/validate-migrations.sh`
- **Rollback Procedures**: `scripts/rollback-migrations.sh`

### Monitoring Setup
- **Alert Rules**: `monitoring/prometheus/transaction-alerts.yml`
- **Grafana Dashboard**: `monitoring/grafana/transaction-performance-dashboard.json`
- **Setup Guide**: `monitoring/monitoring-setup-guide.md`

#### Quick Monitoring Setup
```bash
# Deploy alert rules
cp monitoring/prometheus/transaction-alerts.yml /etc/prometheus/rules/

# Import Grafana dashboard
curl -X POST http://grafana:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @monitoring/grafana/transaction-performance-dashboard.json

# Validate setup
./scripts/validate-migrations.sh
```