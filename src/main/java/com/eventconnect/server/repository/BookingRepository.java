package com.eventconnect.server.repository;

import com.eventconnect.server.entity.Booking;
import com.eventconnect.server.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserEmail(String email);
    
    // Find all bookings for a specific event
    List<Booking> findByEventId(Long eventId);
    
    // Find all active bookings for an event (not cancelled)
    @Query("SELECT b FROM Booking b WHERE b.event.id = :eventId AND b.status != 'CANCELLED'")
    List<Booking> findActiveBookingsByEventId(@Param("eventId") Long eventId);
    
    // Bulk update booking status for an event
    @Modifying
    @Query("UPDATE Booking b SET b.status = :status WHERE b.event.id = :eventId AND b.status != 'CANCELLED'")
    int cancelAllBookingsForEvent(@Param("eventId") Long eventId, @Param("status") BookingStatus status);
}