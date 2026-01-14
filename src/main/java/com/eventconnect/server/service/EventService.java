package com.eventconnect.server.service;

import com.eventconnect.server.dto.EventDto;
import com.eventconnect.server.entity.Booking;
import com.eventconnect.server.entity.BookingStatus;
import com.eventconnect.server.entity.Event;
import com.eventconnect.server.exception.BadRequestException;
import com.eventconnect.server.exception.ResourceNotFoundException;
import com.eventconnect.server.repository.BookingRepository;
import com.eventconnect.server.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;
    private final BookingRepository bookingRepository;

    // Returns a simplified Map structure instead of raw Page object
    public Map<String, Object> getAllEvents(String keyword, Pageable pageable) {
        Page<Event> pageEvents = repository.searchEvents(keyword, LocalDateTime.now(), pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("events", pageEvents.getContent()); // Just the array
        response.put("currentPage", pageEvents.getNumber());
        response.put("totalItems", pageEvents.getTotalElements());
        response.put("totalPages", pageEvents.getTotalPages());

        return response;
    }

    public Event getEventById(Long id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
        
        // Check if event is active
        if (!event.getIsActive()) {
            throw new ResourceNotFoundException("Event has been cancelled or deleted");
        }
        
        return event;
    }

    //CREATE
    @Transactional
    public Event createEvent(EventDto dto) {
        // Validation
        if (repository.existsByTitleAndDateAndLocation(dto.getTitle(), dto.getDate(), dto.getLocation())) {
            throw new BadRequestException("An event with the same title, date, and location already exists.");
        }

        Event event = Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .date(dto.getDate())
                .location(dto.getLocation())
                .category(dto.getCategory())
                .ticketPrice(dto.getTicketPrice())
                .capacity(dto.getCapacity())
                .availableSeats(dto.getCapacity()) // Initially full capacity
                .imageUrl(dto.getImageUrl())
                .build();
        return repository.save(event);
    }

    //UPDATE
    @Transactional
    public Event updateEvent(Long id, EventDto dto) {
        Event event = getEventById(id);

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDate() != null) event.setDate(dto.getDate());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getCategory() != null) event.setCategory(dto.getCategory());
        if (dto.getTicketPrice() != null) event.setTicketPrice(dto.getTicketPrice());
        if (dto.getImageUrl() != null) event.setImageUrl(dto.getImageUrl());

        // Note: We typically don't update 'capacity' easily if bookings exist,
        // but for simplicity, we'll allow it or leave as is.

        return repository.save(event);
    }

    //SOFT DELETE with automatic booking cancellation
    @Transactional
    public void deleteEvent(Long id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
        
        // Check if event is already deleted
        if (!event.getIsActive()) {
            throw new BadRequestException("Event is already deleted");
        }
        
        // Soft delete the event (set isActive to false)
        event.setIsActive(false);
        repository.save(event);
        
        // If event date hasn't passed, cancel all active bookings
        if (event.getDate().isAfter(LocalDateTime.now())) {
            List<Booking> activeBookings = bookingRepository.findActiveBookingsByEventId(id);
            
            if (!activeBookings.isEmpty()) {
                // Cancel all active bookings
                int cancelledCount = bookingRepository.cancelAllBookingsForEvent(id, BookingStatus.CANCELLED);
                log.info("Event {} deleted. Cancelled {} active bookings.", id, cancelledCount);
            } else {
                log.info("Event {} deleted. No active bookings to cancel.", id);
            }
        } else {
            log.info("Event {} deleted. Event date has passed, bookings remain as-is.", id);
        }
    }
    
    // Admin method to get all events including inactive ones
    public Map<String, Object> getAllEventsForAdmin(Pageable pageable) {
        Page<Event> pageEvents = repository.findAllEvents(true, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("events", pageEvents.getContent());
        response.put("currentPage", pageEvents.getNumber());
        response.put("totalItems", pageEvents.getTotalElements());
        response.put("totalPages", pageEvents.getTotalPages());
        
        return response;
    }
}