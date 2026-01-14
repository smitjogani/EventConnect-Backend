package com.eventconnect.server.service;

import com.eventconnect.server.dto.ChangePasswordRequest;
import com.eventconnect.server.dto.UpdateProfileRequest;
import com.eventconnect.server.dto.UserProfileDto;
import com.eventconnect.server.entity.User;
import com.eventconnect.server.exception.BadRequestException;
import com.eventconnect.server.exception.ResourceNotFoundException;
import com.eventconnect.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileDto getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public UserProfileDto updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if new email is already in use by another user
        if (!user.getEmail().equals(request.getEmail())) {
            boolean emailExists = userRepository.findByEmail(request.getEmail()).isPresent();
            if (emailExists) {
                throw new BadRequestException("Email is already in use");
            }
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        User updatedUser = userRepository.save(user);

        return UserProfileDto.builder()
                .id(updatedUser.getId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .username(updatedUser.getUsername())
                .build();
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Check new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        // Check password strength (minimum 6 characters)
        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}