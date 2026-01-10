package com.eventconnect.server.repository;

import com.eventconnect.server.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE " +
            "e.isActive = true AND " + // Only active events
            "e.date > :now AND " + // Only future events
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchEvents(@Param("keyword") String keyword, @Param("now") LocalDateTime now, Pageable pageable);
    
    boolean existsByTitleAndDateAndLocation(String title, LocalDateTime date, String location);
    
    // Find all events including inactive ones (for admin)
    @Query("SELECT e FROM Event e WHERE " +
            "(:includeInactive = true OR e.isActive = true)")
    Page<Event> findAllEvents(@Param("includeInactive") boolean includeInactive, Pageable pageable);
}