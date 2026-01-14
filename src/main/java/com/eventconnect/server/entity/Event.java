package com.eventconnect.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_date", columnList = "date"),
        @Index(name = "idx_event_category", columnList = "category"),
        @Index(name = "idx_event_location", columnList = "location"),
        @Index(name = "idx_event_is_active", columnList = "is_active")
})
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private BigDecimal ticketPrice;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(length = 2083)
    private String imageUrl;

    @Version
    private Long version;
}