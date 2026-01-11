--
-- PostgreSQL database dump - EventConnect Schema
-- Database: event_db
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;

--
-- Name: event_db; Type: DATABASE
--

CREATE DATABASE event_db WITH TEMPLATE = template0 ENCODING = 'UTF8';

\connect event_db

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- =====================================================
-- Table: users
-- =====================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'ADMIN'))
);

-- Index on email for faster lookups
CREATE INDEX idx_users_email ON users(email);

-- =====================================================
-- Table: events
-- =====================================================

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    date TIMESTAMP NOT NULL,
    location VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    ticket_price DECIMAL(10,2) NOT NULL CHECK (ticket_price >= 0),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    available_seats INTEGER NOT NULL CHECK (available_seats >= 0),
    image_url VARCHAR(2083),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for performance
CREATE INDEX idx_event_date ON events(date);
CREATE INDEX idx_event_category ON events(category);
CREATE INDEX idx_event_location ON events(location);
CREATE INDEX idx_event_is_active ON events(is_active);

-- Constraint: available_seats cannot exceed capacity
ALTER TABLE events ADD CONSTRAINT chk_available_seats 
    CHECK (available_seats <= capacity);

-- =====================================================
-- Table: bookings
-- =====================================================

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    booking_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    number_of_tickets INTEGER NOT NULL CHECK (number_of_tickets > 0),
    status VARCHAR(50) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED', 'PENDING')),
    
    -- Foreign Keys
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_event FOREIGN KEY (event_id) 
        REFERENCES events(id) ON DELETE CASCADE
);

-- Indexes for faster queries
CREATE INDEX idx_booking_user_id ON bookings(user_id);
CREATE INDEX idx_booking_event_id ON bookings(event_id);
CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_booking_date ON bookings(booking_date);

-- =====================================================
-- Table: tokens (for JWT refresh tokens)
-- =====================================================

CREATE TABLE tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Foreign Key
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

-- Index for token lookup
CREATE INDEX idx_token_user_id ON tokens(user_id);
CREATE INDEX idx_token_expired ON tokens(expired);

-- =====================================================
-- Sample Data (Optional - for testing)
-- =====================================================

-- Insert Admin User
-- Credentials: admin@gmail.com / password123
-- Password is BCrypt hashed with strength 10
INSERT INTO users (name, email, password, role) VALUES
('Admin', 'admin@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IVI9jZ.qPZRQJbSQ5YrHFqJNQ6LjKu', 'ADMIN');

-- Insert Regular User (optional - for testing)
-- Credentials: user@example.com / password123
INSERT INTO users (name, email, password, role) VALUES
('John Doe', 'user@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye/IVI9jZ.qPZRQJbSQ5YrHFqJNQ6LjKu', 'USER');

-- Insert Sample Events
INSERT INTO events (title, description, date, location, category, ticket_price, capacity, available_seats, image_url, is_active) VALUES
('Full Stack Developer Bootcamp', 'Learn MERN stack development from scratch', '2026-01-31 10:00:00', 'Mumbai, India', 'Technology', 100.00, 50, 50, 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97', TRUE),
('Startup Pitch Night', 'Present your startup idea to investors', '2026-02-20 18:00:00', 'Bangalore, India', 'Business', 99.99, 100, 100, 'https://images.unsplash.com/photo-1559136555-9303baea8ebd', TRUE),
('Urban Art & Design Expo', 'Explore contemporary art and design', '2026-03-01 09:00:00', 'Delhi, India', 'Art', 50.00, 200, 200, 'https://images.unsplash.com/photo-1561214115-f2f134cc4912', TRUE),
('Neon Dreams V2 (Rescheduled)', 'Electronic music festival', '2026-03-15 20:00:00', 'Goa, India', 'Music', 120.00, 500, 500, 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3', TRUE);

-- =====================================================
-- Views (Optional - for reporting)
-- =====================================================

-- View: Active Events with Booking Count
CREATE OR REPLACE VIEW v_active_events_summary AS
SELECT 
    e.id,
    e.title,
    e.date,
    e.location,
    e.category,
    e.ticket_price,
    e.capacity,
    e.available_seats,
    (e.capacity - e.available_seats) AS tickets_sold,
    COUNT(b.id) AS total_bookings,
    SUM(CASE WHEN b.status = 'CONFIRMED' THEN b.number_of_tickets ELSE 0 END) AS confirmed_tickets
FROM events e
LEFT JOIN bookings b ON e.id = b.event_id
WHERE e.is_active = TRUE
GROUP BY e.id;

-- View: User Booking History
CREATE OR REPLACE VIEW v_user_bookings AS
SELECT 
    u.id AS user_id,
    u.name AS user_name,
    u.email,
    e.id AS event_id,
    e.title AS event_title,
    e.date AS event_date,
    b.booking_date,
    b.number_of_tickets,
    b.status,
    (e.ticket_price * b.number_of_tickets) AS total_amount
FROM users u
JOIN bookings b ON u.id = b.user_id
JOIN events e ON b.event_id = e.id;

-- =====================================================
-- Functions (Optional - for business logic)
-- =====================================================

-- Function: Check seat availability before booking
CREATE OR REPLACE FUNCTION check_seat_availability(
    p_event_id BIGINT,
    p_num_tickets INTEGER
) RETURNS BOOLEAN AS $$
DECLARE
    v_available_seats INTEGER;
BEGIN
    SELECT available_seats INTO v_available_seats
    FROM events
    WHERE id = p_event_id AND is_active = TRUE;
    
    RETURN v_available_seats >= p_num_tickets;
END;
$$ LANGUAGE plpgsql;

-- Function: Update available seats after booking
CREATE OR REPLACE FUNCTION update_available_seats()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW.status = 'CONFIRMED') THEN
        UPDATE events
        SET available_seats = available_seats - NEW.number_of_tickets
        WHERE id = NEW.event_id;
    ELSIF (TG_OP = 'UPDATE' AND OLD.status = 'CONFIRMED' AND NEW.status = 'CANCELLED') THEN
        UPDATE events
        SET available_seats = available_seats + OLD.number_of_tickets
        WHERE id = OLD.event_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Auto-update available seats
CREATE TRIGGER trg_update_seats
AFTER INSERT OR UPDATE ON bookings
FOR EACH ROW
EXECUTE FUNCTION update_available_seats();

-- =====================================================
-- Grants (Optional - for security)
-- =====================================================

-- Create application user (if not using default postgres)
-- CREATE USER eventconnect_app WITH PASSWORD 'your_secure_password';
-- GRANT CONNECT ON DATABASE event_db TO eventconnect_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO eventconnect_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO eventconnect_app;

-- =====================================================
-- Comments (Documentation)
-- =====================================================

COMMENT ON TABLE users IS 'Stores user authentication and profile information';
COMMENT ON TABLE events IS 'Stores event details with soft delete support';
COMMENT ON TABLE bookings IS 'Stores ticket booking records';
COMMENT ON TABLE tokens IS 'Stores JWT refresh tokens for authentication';

COMMENT ON COLUMN events.is_active IS 'Soft delete flag - FALSE means event is deleted';
COMMENT ON COLUMN events.deleted_at IS 'Timestamp when event was soft deleted';
COMMENT ON COLUMN events.version IS 'Optimistic locking version for concurrent updates';

-- =====================================================
-- End of Schema
-- =====================================================

-- Verify tables created
SELECT 
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) AS column_count
FROM information_schema.tables t
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;
