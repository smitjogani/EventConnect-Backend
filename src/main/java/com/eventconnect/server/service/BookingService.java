package com.eventconnect.server.service;

import com.eventconnect.server.dto.BookingRequest;
import com.eventconnect.server.dto.BookingResponseDto;
import com.eventconnect.server.entity.Booking;
import com.eventconnect.server.entity.BookingStatus;
import com.eventconnect.server.entity.Event;
import com.eventconnect.server.entity.User;
import com.eventconnect.server.exception.BadRequestException;
import com.eventconnect.server.exception.ResourceNotFoundException;
import com.eventconnect.server.repository.BookingRepository;
import com.eventconnect.server.repository.EventRepository;
import com.eventconnect.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RateLimiterService rateLimiterService;
    private final LocationService locationService;

    // --- Create Booking ---
    @Transactional
    public BookingResponseDto bookTickets(String userEmail, BookingRequest request, HttpServletRequest httpRequest) {
        // 1. Extract IP
        String clientIp = locationService.extractClientIp(httpRequest);

        // 2. Rate Limiting
        rateLimiterService.checkRateLimit(userEmail);

        // 3. Validate Inputs
        if (request.getTickets() <= 0) {
            throw new BadRequestException("Ticket count must be positive.");
        }
        
        // Validate location is provided (mandatory)
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException("Location permission is required to book tickets. Please enable location access and try again.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot book tickets for a past event.");
        }
        if (event.getAvailableSeats() < request.getTickets()) {
            throw new BadRequestException("Not enough seats available. Only " + event.getAvailableSeats() + " left.");
        }

        // 4. Update Inventory
        event.setAvailableSeats(event.getAvailableSeats() - request.getTickets());
        eventRepository.save(event);

        // 5. Get location name from coordinates using reverse geocoding
        String locationName = locationService.getLocationFromCoordinates(
            request.getLatitude(), 
            request.getLongitude()
        );

        // 6. Save Booking with IP, coordinates, and location name
        Booking booking = Booking.builder()
                .user(user)
                .event(event)
                .bookingDate(LocalDateTime.now())
                .numberOfTickets(request.getTickets())
                .status(BookingStatus.CONFIRMED)
                .ipAddress(clientIp)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(locationName)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Return DTO (Not Entity)
        return mapToDto(savedBooking);
    }

    //Get My Bookings
    public List<BookingResponseDto> getUserBookings(String userEmail) {
        List<Booking> bookings = bookingRepository.findByUserEmail(userEmail);
        return bookings.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    //Get Booking By ID
    public BookingResponseDto getBookingById(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Security Check: Ensure the user actually owns this booking!
        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You are not authorized to view this booking.");
        }

        return mapToDto(booking);
    }

    // Helper to convert Entity -> DTO
    private BookingResponseDto mapToDto(Booking booking) {
        return BookingResponseDto.builder()
                .bookingId(booking.getId())
                .eventTitle(booking.getEvent().getTitle())
                .eventDate(booking.getEvent().getDate())
                .eventLocation(booking.getEvent().getLocation())
                .tickets(booking.getNumberOfTickets())
                // Calculate Total Price (Price * Tickets)
                .totalAmount(booking.getEvent().getTicketPrice().multiply(BigDecimal.valueOf(booking.getNumberOfTickets())))
                .status(booking.getStatus())
                .bookingDate(booking.getBookingDate())
                .ipAddress(booking.getIpAddress())
                .latitude(booking.getLatitude())
                .longitude(booking.getLongitude())
                .location(booking.getLocation())
                .build();
    }
}