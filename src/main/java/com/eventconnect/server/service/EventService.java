package com.eventconnect.server.service;

import com.eventconnect.server.dto.EventDto;
import com.eventconnect.server.entity.Event;
import com.eventconnect.server.exception.BadRequestException;
import com.eventconnect.server.exception.ResourceNotFoundException;
import com.eventconnect.server.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;

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
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
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
                .version(0L)
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

    //DELETE
    public void deleteEvent(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Event not found with id: " + id);
        }
        repository.deleteById(id);
    }
}