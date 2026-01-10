# Event Connect - Backend API

Event Connect is a robust, scalable RESTful API built with **Spring Boot 3** for managing events and ticket bookings. It features secure JWT authentication, role-based access control, optimistic locking for concurrency, and custom rate limiting.

## 🚀 Tech Stack

*   **Core Framework**: Spring Boot 3.3.0 (Java 17)
*   **Database**: PostgreSQL 15+
*   **ORM**: Hibernate / Spring Data JPA
*   **Security**: Spring Security 6 + JWT (JJWT Library)
*   **Build Tool**: Maven

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
