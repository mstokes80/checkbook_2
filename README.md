# Checkbook Application

A family checkbook balancing application with Spring Boot backend and React frontend.

## Architecture

- **Backend**: Spring Boot 3.x with JWT authentication
- **Frontend**: React 18+ with Vite and TailwindCSS
- **Database**: PostgreSQL 17+
- **Email**: MailHog (development) / SMTP (production)
- **Containerization**: Docker & Docker Compose

## Quick Start with Docker

### Prerequisites
- Docker and Docker Compose installed
- Git

### Development Environment

1. Clone the repository:
```bash
git clone <your-repo-url>
cd checkbook
```

2. Copy environment file:
```bash
cp .env.example .env
```

3. Start all services:
```bash
docker-compose up -d
```

4. Access the application:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **MailHog UI**: http://localhost:8025
- **PostgreSQL**: localhost:5432

### Services

| Service | Description | URL | Credentials |
|---------|-------------|-----|-------------|
| UI | React Frontend | http://localhost:3000 | - |
| API | Spring Boot Backend | http://localhost:8080/api | - |
| PostgreSQL | Database | localhost:5432 | user: checkbook_user, pass: checkbook_password, db: checkbook |
| MailHog | Email Testing | http://localhost:8025 | - |

### Database Migrations

Flyway migrations run automatically on startup. Migration files are located in:
```
checkbook-api/src/main/resources/db/migration/
```

### Email Testing

All emails are captured by MailHog during development:
- SMTP Server: localhost:1025
- Web Interface: http://localhost:8025

## Development Commands

### Docker Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up --build

# Remove volumes (reset database)
docker-compose down -v

# Start only database and email
docker-compose up -d postgres mailhog
```

### Individual Service Development

#### Backend (Spring Boot)
```bash
cd checkbook-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Frontend (React)
```bash
cd checkbook-ui
npm install
npm run dev
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password
- `GET /api/auth/me` - Get current user
- `PUT /api/auth/profile` - Update user profile
- `POST /api/auth/logout` - Logout user

### Health Checks
- `GET /api/health` - Simple health check
- `GET /api/actuator/health` - Detailed health information

## Environment Variables

Key environment variables (see `.env.example`):

```bash
# Database
POSTGRES_DB=checkbook
POSTGRES_USER=checkbook_user
POSTGRES_PASSWORD=checkbook_password

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# URLs
APP_BASE_URL=http://localhost:3000
VITE_API_BASE_URL=http://localhost:8080/api
```

## Database Schema

The application uses Flyway for database migrations. Key tables:

- `users` - User accounts with authentication
- `password_reset_tokens` - Password reset tokens
- Additional tables for checkbook functionality (to be added)

## Security Features

- JWT-based authentication with 24-hour expiration
- BCrypt password hashing (strength 12)
- CORS configuration for cross-origin requests
- Rate limiting on password reset requests
- SQL injection protection via JPA/Hibernate

## Technology Stack

### Backend
- Spring Boot 3.x
- Spring Security 6.x
- Spring Data JPA
- PostgreSQL Driver
- Flyway (database migrations)
- Java Mail Sender
- JWT (io.jsonwebtoken)
- BCrypt password encoding

### Frontend
- React 18+
- Vite (build tool)
- React Router 6+
- Axios (HTTP client)
- TailwindCSS (styling)
- React Hook Form (form handling)

### DevOps
- Docker & Docker Compose
- NGINX (reverse proxy)
- MailHog (email testing)
- PostgreSQL (database)

## Production Deployment

For production deployment, use the production profile:

```bash
docker-compose --profile production up -d
```

This includes:
- NGINX reverse proxy with SSL termination
- Production-optimized database settings
- Real SMTP email configuration
- Security headers and optimizations

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 3000, 8080, 5432, 1025, 8025 are available
2. **Database connection**: Check PostgreSQL is running and accessible
3. **Email not working**: Verify MailHog is running on port 1025
4. **Frontend not loading**: Check API_BASE_URL environment variable

### Logs

View service logs:
```bash
docker-compose logs -f api
docker-compose logs -f ui
docker-compose logs -f postgres
```

### Reset Environment

To completely reset the development environment:
```bash
docker-compose down -v
docker system prune -f
docker-compose up --build
```

## Contributing

1. Follow existing code style and patterns
2. Ensure all tests pass
3. Update documentation for new features
4. Use conventional commit messages

## License

[Add your license here]