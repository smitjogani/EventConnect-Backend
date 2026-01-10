package com.eventconnect.server.service;

import com.eventconnect.server.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    @Value("${app.rate-limit.max-bookings:5}")
    private int maxBookings;

    @Value("${app.rate-limit.duration-sec:60}")
    private int durationSeconds;

    // Stores timestamps of bookings for each user: Map<UserEmail, List<Timestamp>>
    private final Map<String, List<LocalDateTime>> userRequestHistory = new ConcurrentHashMap<>();

    public void checkRateLimit(String userEmail) {
        List<LocalDateTime> history = userRequestHistory.computeIfAbsent(userEmail, k -> new ArrayList<>());
        LocalDateTime now = LocalDateTime.now();

        synchronized (history) {
            // 1. Remove old timestamps (outside the window)
            history.removeIf(timestamp -> timestamp.isBefore(now.minusSeconds(durationSeconds)));

            // 2. Check strict limit
            if (history.size() >= maxBookings) {
                throw new RateLimitExceededException("Rate limit exceeded. You can only make "
                        + maxBookings + " bookings per " + durationSeconds + " seconds.");
            }

            // 3. Add current request
            history.add(now);
        }
    }
}