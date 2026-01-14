package com.eventconnect.server.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileDto {
    private Long id;
    private String name;
    private String email;
    private String username;
    
    // Audit fields
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
    private Boolean isActive;
}
