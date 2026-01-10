package com.eventconnect.server.controller;

import com.eventconnect.server.dto.BookingRequest;
import com.eventconnect.server.dto.BookingResponseDto;
import com.eventconnect.server.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> bookTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody BookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.bookTickets(userDetails.getUsername(), request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(bookingService.getUserBookings(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingService.getBookingById(id, userDetails.getUsername()));
    }
}