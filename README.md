# Event Connect - Backend API

Event Connect is a robust, scalable RESTful API built with **Spring Boot 3** for managing events and ticket bookings. It features secure JWT authentication, role-based access control, optimistic locking for concurrency, and custom rate limiting.

## 🚀 Tech Stack

*   **Core Framework**: Spring Boot 3.3.0 (Java 17)
*   **Database**: PostgreSQL 15+
*   **ORM**: Hibernate / Spring Data JPA
*   **Security**: Spring Security 6 + JWT (JJWT Library)
*   **Build Tool**: Maven

---

## 🏗 System Architecture

The application follows a standard **Layered Architecture** to ensure separation of concerns:

1.  **Controller Layer (`/controller`)**: Handles HTTP requests, validates input DTOs, and returns standardized JSON responses.
2.  **Service Layer (`/service`)**: Contains business logic (e.g., preventing double bookings, rate limiting, token generation).
3.  **Repository Layer (`/repository`)**: Interfaces with the Database using Spring Data JPA.
4.  **Security Layer (`/security`)**: A filter chain that intercepts requests to validate JWT tokens before they reach the controllers.

### Key Design Decisions

*   **Stateless Authentication**: We use **JWT (JSON Web Tokens)** instead of server-side sessions. This allows the backend to be horizontally scaled easily without "sticky sessions".
*   **Optimistic Locking**: The `Event` entity has a `@Version` field. This prevents "Double Booking" issues where two users try to buy the last ticket simultaneously. The database ensures only one write succeeds.
*   **Sliding Window Rate Limiting**: To prevent API abuse, we implemented a custom in-memory rate limiter `RateLimiterService` that restricts users to X bookings per Y seconds.
*   **Database Indexing**: The `events` table uses `@Index` on `date`, `location`, and `category` fields to ensure high-performance search and filtering.

---

## 🛠 Database Schema

### 1. `_user` Table
*   Stores user credentials.
*   **Why `_user`?**: "user" is a reserved keyword in PostgreSQL, so we escaped it.
*   **Fields**: `id`, `name`, `email` (Unique), `password` (BCrypt Hash), `role` (USER/ADMIN).

### 2. `events` Table
*   Stores event details and inventory.
*   **Indexing**: Indexed by `date`, `category`, and `location` for fast `LIKE %...%` searches.
*   **Concurrency**: Includes `available_seats` and `version` column.

### 3. `bookings` Table
*   Links `_user` and `events`.
*   **Logic**: A user cannot book past events or more tickets than available.

---

## ⚙️ Setup & Installation

### 1. Prerequisites
*   Java Development Kit (JDK) 17+
*   PostgreSQL running on port `5432` (or custom port).

### 2. Configuration
Create a file named `src/main/resources/application.properties` (if not exists) or use environment variables.
**Note**: This file is git-ignored for security.

```properties
# standard db config
spring.datasource.url=jdbc:postgresql://localhost:5433/event_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_DB_PASSWORD

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT Configuration
app.jwt.secret=YOUR_VERY_LONG_SECRET_KEY_HERE
app.jwt.expiration-ms=900000      # 15 mins
app.jwt.refresh-expiration-ms=86400000  # 24 hours

# Rate Limiting
app.rate-limit.max-bookings=5
app.rate-limit.duration-sec=60
```

### 3. Running the App
```bash
./mvnw spring-boot:run
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
| `GET` | `/api/v1/events` | Get paginated events. Params: `keyword`, `page`, `size` | No |
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

---

## 🚨 Error Handling

The API uses a **Global Exception Handler** (`@RestControllerAdvice`) to return consistent JSON errors:

```json
{
    "timestamp": "2026-01-10T12:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Not enough seats available. Only 2 left."
}
```

*   **400 Bad Request**: Validation errors, Sold out, Duplicate email.
*   **401 Unauthorized**: Missing or Invalid JWT Token.
*   **403 Forbidden**: Accessing Admin route as User.
*   **404 Not Found**: Event/Booking ID does not exist.
*   **429 Too Many Requests**: Rate limit exceeded (Max 5/min).
