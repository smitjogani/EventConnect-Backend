package com.eventconnect.server.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    @NotNull(message = "Number of tickets is required")
    private Integer tickets;
    
    @NotNull(message = "Location permission is required. Please enable location access to book tickets.")
    private Double latitude;
    
    @NotNull(message = "Location permission is required. Please enable location access to book tickets.")
    private Double longitude;
}