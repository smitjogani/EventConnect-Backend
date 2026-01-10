package com.eventconnect.server.dto;
import lombok.Data;

@Data
public class BookingRequest {
    private Long eventId;
    private Integer tickets;
}