# Database Setup and Configuration

This document provides comprehensive instructions for setting up and configuring the PostgreSQL database for the Checkbook API authentication system.

## Prerequisites

- PostgreSQL 17+ installed and running
- Java 17+ for running the Spring Boot application
- Maven for dependency management

## Database Configuration

### 1. Database Creation

Connect to PostgreSQL as a superuser and create the database and user:

```sql
-- Connect to PostgreSQL as superuser
psql -U postgres

-- Create database
CREATE DATABASE checkbook_db;

-- Create user with appropriate permissions
CREATE USER checkbook_user WITH PASSWORD 'checkbook_password';

-- Grant necessary privileges
GRANT ALL PRIVILEGES ON DATABASE checkbook_db TO checkbook_user;

-- Connect to the checkbook database
\c checkbook_db

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO checkbook_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO checkbook_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO checkbook_user;

-- Ensure future objects are also accessible
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO checkbook_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO checkbook_user;
```

### 2. Application Configuration

The database connection is configured in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/checkbook_db
    username: checkbook_user
    password: checkbook_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Important: Let Flyway handle schema management
    show-sql: false       # Set to true for debugging
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    open-in-view: false

  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
```

### 3. Environment-Specific Configuration

For different environments, you can override database settings:

#### Development (application-dev.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/checkbook_dev
    username: checkbook_user
    password: checkbook_password
  jpa:
    show-sql: true
  flyway:
    clean-disabled: false
```

#### Production (application-prod.yml)
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/checkbook_db}
    username: ${DATABASE_USERNAME:checkbook_user}
    password: ${DATABASE_PASSWORD}
  jpa:
    show-sql: false
  flyway:
    clean-disabled: true
    validate-on-migrate: true
```

## Flyway Migration Management

### Migration File Structure

Migrations are located in `src/main/resources/db/migration/`:

- `V1__create_user_table.sql` - Core user authentication table
- `V2__create_password_reset_tokens.sql` - Password reset and session management
- `V3__add_performance_indexes.sql` - Performance indexes and constraints

### Migration Commands

```bash
# Run migrations (automatic on application startup)
mvn spring-boot:run

# Run Flyway migrations manually
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Clean database (development only)
mvn flyway:clean
```

### Migration Best Practices

1. **Never modify existing migrations** - Always create new migrations for changes
2. **Test migrations thoroughly** - Validate on a copy of production data
3. **Keep migrations small** - One logical change per migration
4. **Use descriptive names** - Clear naming helps with maintenance

## Database Schema Overview

### Core Tables

#### users
- Primary authentication table
- Stores user credentials and account status
- Includes security fields for account locking and login tracking

#### password_reset_tokens
- Secure token management for password resets
- Includes expiration and usage tracking
- IP and user agent logging for security

#### jwt_blacklist
- Manages invalidated JWT tokens
- Supports secure logout functionality
- Automatic cleanup of expired tokens

#### user_sessions
- Active session tracking
- Device and location management
- Session lifecycle management

### Performance Optimizations

- **Indexes**: Optimized for common authentication queries
- **Constraints**: Data integrity and validation rules
- **Cleanup Functions**: Automatic removal of expired data
- **Triggers**: Automatic timestamp updates

## Security Considerations

1. **Password Storage**: BCrypt hashing with strength 12
2. **Token Security**: Cryptographically secure random tokens
3. **Session Management**: 24-hour JWT expiration with refresh capability
4. **Account Security**: Failed login attempt tracking and account locking
5. **Data Cleanup**: Automatic removal of expired tokens and sessions

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure PostgreSQL is running and accessible
2. **Authentication Failed**: Verify username/password in application.yml
3. **Migration Failures**: Check SQL syntax and database permissions
4. **Permission Denied**: Ensure user has proper database privileges

### Useful Commands

```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Connect to database
psql -U checkbook_user -d checkbook_db

# View migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

# Check table structure
\d users
\d password_reset_tokens
```

## Backup and Recovery

### Backup Commands
```bash
# Full database backup
pg_dump -U checkbook_user -h localhost checkbook_db > checkbook_backup.sql

# Schema-only backup
pg_dump -U checkbook_user -h localhost --schema-only checkbook_db > checkbook_schema.sql
```

### Recovery Commands
```bash
# Restore from backup
psql -U checkbook_user -d checkbook_db < checkbook_backup.sql
```

## Maintenance

### Regular Cleanup
The database includes an automatic cleanup function that should be run periodically:

```sql
-- Manual cleanup of expired data
SELECT cleanup_expired_auth_data();
```

Consider setting up a cron job to run this function daily:
```bash
# Add to crontab (crontab -e)
0 2 * * * psql -U checkbook_user -d checkbook_db -c "SELECT cleanup_expired_auth_data();"
```