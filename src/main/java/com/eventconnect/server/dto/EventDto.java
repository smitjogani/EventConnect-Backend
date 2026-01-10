package com.eventconnect.server.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private String category;
    private BigDecimal ticketPrice;
    private Integer capacity;
    private Integer availableSeats;
    private String imageUrl;
}