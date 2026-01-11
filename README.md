# Event Connect - Backend API

> RESTful API built with Spring Boot for event management and ticket booking

---

## 📋 Table of Contents

- [Quick Setup](#quick-setup)
- [Assumptions](#assumptions)
- [Design Notes](#design-notes)
- [Authentication Strategy](#authentication-strategy)
- [Tech Stack](#tech-stack)
- [API Documentation](#api-documentation)
- [Database Design](#database-design)

---

## 🚀 Quick Setup

### Prerequisites
- Java 17+ (JDK)
- Maven 3.8+
- PostgreSQL 12+

### Installation

```bash
# Navigate to Server directory
cd Server

# Create application.properties
cat > src/main/resources/application.properties << EOF
spring.datasource.url=jdbc:postgresql://localhost:5432/event_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

jwt.secret=your-super-secret-jwt-key-at-least-256-bits-long-please
jwt.expiration=900000
jwt.refresh-expiration=604800000

server.port=8080
EOF

# Install dependencies and run
mvn clean install
mvn spring-boot:run
```

**Access**: http://localhost:8080/api/v1

### Database Setup

```bash
# Create database
psql -U postgres
CREATE DATABASE event_db;
\q

# Run schema (creates tables + admin user)
psql -U postgres -d event_db -f schema_dump.sql
```

**Admin Credentials**:
- Email: `admin@gmail.com`
- Password: `password123`

---

## 🌐 Deployment

### Live Production URL
🔗 **Backend API**: [https://eventconnect-backend-production.up.railway.app](https://eventconnect-backend-production.up.railway.app)

### Deployment Platform: Railway

The backend is deployed on **Railway** with the following configuration:

#### Environment Variables
Set these in Railway Dashboard → Backend Service → Variables:

```env
# Database (Auto-provided by Railway PostgreSQL)
PGHOST=<railway-postgres-host>
PGPORT=5432
PGDATABASE=<database-name>
PGUSER=<database-user>
PGPASSWORD=<database-password>

# JWT Configuration
JWT_SECRET=<base64-encoded-secret-key>
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# CORS (Frontend URL)
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://eventconnectbook.netlify.app

# Server Port (Auto-provided by Railway)
PORT=8080
```

#### Deployment Steps

1. **Connect GitHub Repository**
   - Go to [Railway Dashboard](https://railway.app)
   - Create new project → Deploy from GitHub
   - Select `EventConnect-Backend` repository
   - Railway auto-detects Spring Boot and builds with Maven

2. **Add PostgreSQL Database**
   - In Railway project → Add → Database → PostgreSQL
   - Railway automatically creates database and provides connection variables

3. **Link Database to Backend**
   - Go to Backend service → Variables tab
   - Add variable references:
     ```
     PGHOST = ${{Postgres.PGHOST}}
     PGPORT = ${{Postgres.PGPORT}}
     PGDATABASE = ${{Postgres.PGDATABASE}}
     PGUSER = ${{Postgres.PGUSER}}
     PGPASSWORD = ${{Postgres.PGPASSWORD}}
     ```

4. **Configure JWT Secret**
   - Generate a Base64-encoded secret:
     ```bash
     echo -n "your-secret-key" | base64
     ```
   - Add to Railway variables:
     ```
     JWT_SECRET=<base64-encoded-value>
     ```

5. **Set Production Profile**
   ```
   SPRING_PROFILES_ACTIVE=prod
   ```

6. **Generate Public Domain**
   - Go to Backend service → Settings → Networking
   - Click "Generate Domain"
   - Your API will be available at: `https://<your-service>.up.railway.app`

7. **Populate Database**
   - Go to Postgres service → Data tab
   - Run the SQL from `schema_dump.sql` to create tables and admin user

#### Production Configuration

The production configuration is in `src/main/resources/application-prod.properties`:

```properties
# Database - Uses Railway environment variables
spring.datasource.url=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
spring.datasource.username=${PGUSER}
spring.datasource.password=${PGPASSWORD}

# JWT
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=${JWT_EXPIRATION}
app.jwt.refresh-expiration-ms=${JWT_REFRESH_EXPIRATION}

# CORS
cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
```

#### Health Check

Test the deployment:
```bash
curl https://eventconnect-backend-production.up.railway.app/api/v1/events
```

Expected response:
```json
{
  "totalItems": 0,
  "totalPages": 0,
  "currentPage": 0,
  "events": []
}
```

---

## 📝 Assumptions

### Data Assumptions
- Event dates are stored in UTC
- Prices are in decimal format (10,2)
- Email addresses are unique
- User passwords are BCrypt hashed (strength 10)
- Event capacity is always positive
- Available seats never exceed capacity

### Business Logic Assumptions
- Users can book multiple tickets per event
- Bookings are final (no cancellation by users)
- Admin can delete events (soft delete)
- Deleted events automatically cancel future bookings
- Past event bookings are preserved for history
- One currency (INR) for all transactions

### Security Assumptions
- JWT tokens are used for authentication
- Access tokens expire in 15 minutes
- Refresh tokens expire in 7 days
- HTTPS is used in production
- CORS is configured for frontend origin
- All passwords are hashed before storage
- SQL injection prevented by JPA/Hibernate

### Technical Assumptions
- PostgreSQL is the database
- Frontend consumes JSON responses
- Pagination is required for large datasets
- Optimistic locking prevents race conditions
- Database handles referential integrity

---

## 🎯 Design Notes

### Architecture Decisions

#### **1. Layered Architecture**
**Why**: Clear separation of concerns
```
Controller → Service → Repository → Database
```
- **Controller**: HTTP handling, validation
- **Service**: Business logic, transactions
- **Repository**: Data access
- **Entity**: Database mapping

**Benefits**:
- Easy to test
- Easy to maintain
- Clear responsibilities
- Scalable structure

#### **2. JWT Authentication**
**Why**: Stateless, scalable authentication
- No session storage needed
- Works well with microservices
- Mobile-friendly
- Horizontal scaling

**Trade-offs**:
- Token size larger than session ID
- Cannot revoke tokens easily
- Refresh token complexity

#### **3. Soft Delete for Events**
**Why**: Data preservation and audit trail
- Preserves historical data
- Allows recovery
- Maintains referential integrity
- Audit trail for compliance

**Implementation**:
```java
@Column(name = "is_active")
private Boolean isActive = true;

@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

#### **4. Optimistic Locking**
**Why**: Prevents double booking
```java
@Version
private Long version;
```
- Prevents concurrent updates
- Better performance than pessimistic locking
- Handles race conditions

#### **5. Spring Data JPA**
**Why**: Reduces boilerplate
- Auto-generates queries
- Type-safe
- Pagination support
- Custom queries when needed

**Trade-offs**:
- Learning curve
- Less control over SQL
- Potential N+1 queries

### Database Design Decisions

#### **Indexes**
```sql
CREATE INDEX idx_event_date ON events(date);
CREATE INDEX idx_event_category ON events(category);
CREATE INDEX idx_event_is_active ON events(is_active);
CREATE INDEX idx_users_email ON users(email);
```
**Why**: Faster queries on frequently searched columns

#### **Constraints**
```sql
CHECK (ticket_price >= 0)
CHECK (capacity > 0)
CHECK (available_seats <= capacity)
```
**Why**: Data integrity at database level

#### **Foreign Keys**
```sql
CONSTRAINT fk_booking_user FOREIGN KEY (user_id) 
    REFERENCES users(id) ON DELETE CASCADE
```
**Why**: Referential integrity, automatic cleanup

### API Design Decisions

#### **RESTful Endpoints**
- `GET /events` - List events
- `POST /events` - Create event
- `PUT /events/{id}` - Update event
- `DELETE /events/{id}` - Soft delete event

**Why**: Standard, predictable, cacheable

#### **Pagination**
```
GET /events?page=0&size=10&keyword=music
```
**Why**: Performance, user experience

#### **Error Handling**
```json
{
  "timestamp": "2026-01-11T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Event not found",
  "path": "/api/v1/events/999"
}
```
**Why**: Consistent error responses

---

## 🛠️ Tech Stack

### Core Framework
- **Spring Boot**: 3.3.0
- **Java**: 17+
- **Maven**: 3.8+

### Database
- **PostgreSQL**: 15+
- **Hibernate/JPA**: ORM
- **HikariCP**: Connection pooling

### Security
- **Spring Security**: 6.x
- **JWT (JJWT)**: 0.11.5
- **BCrypt**: Password hashing

### Validation
- **Bean Validation**: JSR-303
- **Hibernate Validator**: Implementation

### Development Tools
- **Lombok**: Reduce boilerplate
- **Spring Boot DevTools**: Hot reload

---

## 📂 Project Structure

```
src/main/java/com/eventconnect/server
├── config/             # App Configuration (Security, Beans, Filters)
│   ├── ApplicationConfig.java      # Beans for AuthManager, PasswordEncoder
│   ├── JwtAuthenticationFilter.java # Intercepts requests to check Tokens
│   └── SecurityConfig.java         # Rules (Who can access what URL)
│
├── controller/         # API Endpoints (REST Controllers)
│   ├── AuthController.java
│   └── ...
├── dto/                # Data Transfer Objects
├── entity/             # Database Tables
├── exception/          # Global Error Handling
├── repository/         # Database Access
├── security/           # JWT Logic
└── service/            # Business Logic
```

---

## 🔐 Authentication Strategy: The "Double Token" System

Our project uses a modern **Access Token + Refresh Token** strategy. This achieves the perfect balance between **User Experience** and **Security**.

### 1. The Problem
*   If a token lasts forever -> **Security Risk** (If stolen, hacker has access forever).
*   If a token lasts 10 minutes -> **Bad UX** (User takes 15 mins to browse events, tries to book, and gets "Please Login Again").

### 2. The Solution (How we use it)
We issue TWO tokens at login:

#### A. Access Token (Short-lived: 15 mins)
*   **Role**: The "Entry Ticket".
*   **Usage**: Sent in the Header (`Authorization: Bearer xyz`) for every API call (Booking, Browsing).
*   **Security**: Because it expires quickly, if a hacker intercepts it on public WiFi, they only have access for a few minutes.

#### B. Refresh Token (Long-lived: 24 hours)
*   **Role**: The "Renewal Ticket".
*   **Usage**: Kept secretly by the Client. NEVER sent for regular API calls.
*   **Workflow**: 
    1.  Client tries to Book Ticket with Access Token.
    2.  Server returns `403 Forbidden` (Token Expired).
    3.  Client (JavaScript) catches this error securely.
    4.  Client calls `/auth/refresh-token` (Future Implementation) sending the **Refresh Token**.
    5.  Server verifies the Refresh Token.
    6.  Server issues a **NEW Access Token**.
    7.  Client retries the Booking request.
*   **Result**: The user never sees a "Login Again" screen, but security remains high.

---

## 🏗 System Architecture

The application follows a standard **Layered Architecture**:
1.  **Controller**: Receives HTTP, Validates DTO.
2.  **Service**: Executes Logic (Rate limit check, Sold out check).
3.  **Repository**: Runs SQL queries.
4.  **Database**: Stores data strictly.

### Key Design Decisions
*   **Optimistic Locking**: `Event` entity has a `@Version` field to prevent "Double Booking".
*   **Rate Limiting**: Custom in-memory service restricts users to X bookings per Y seconds.
*   **Database Indexing**: `events` table sends `@Index` on `date`, `category`, and `location`.

---

## ⚙️ Setup & Installation

### Configuration
Create `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/event_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update

# Security Secrets
app.jwt.secret=YOUR_LONG_SECRET_KEY
app.jwt.expiration-ms=900000
app.jwt.refresh-expiration-ms=86400000
```

---

## 📡 API Documentation

### 🔐 Authentication

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Create a new account | No |
| `POST` | `/api/v1/auth/authenticate` | Login to get Access/Refresh tokens | No |

### 📅 Events

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/events` | Get events. Params: `keyword`, `page`, `size` | No |
| `GET` | `/api/v1/events/{id}` | Get single event details | No |
| `POST` | `/api/v1/events` | Create a new event | **Yes (ADMIN)** |
| `PUT` | `/api/v1/events/{id}` | Update an event | **Yes (ADMIN)** |
| `DELETE` | `/api/v1/events/{id}` | Delete an event | **Yes (ADMIN)** |

### 🎟 Bookings

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/bookings` | Book tickets. Payload: `{ eventId, tickets }` | **Yes (USER)** |
| `GET` | `/api/v1/bookings/my-bookings` | Get booking history for logged-in user | **Yes (USER)** |
| `GET` | `/api/v1/bookings/{id}` | Get specific booking receipt | **Yes (Owner)** |
