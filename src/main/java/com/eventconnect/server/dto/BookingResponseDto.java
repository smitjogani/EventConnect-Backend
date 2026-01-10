package com.eventconnect.server.dto;

import com.eventconnect.server.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponseDto {
    private Long bookingId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;
    private Integer tickets;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime bookingDate;
}