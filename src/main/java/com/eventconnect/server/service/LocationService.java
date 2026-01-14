package com.eventconnect.server.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Extract client IP address from HTTP request
     */
    public String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        
        if (clientIp != null && !clientIp.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            clientIp = clientIp.split(",")[0].trim();
        } else {
            clientIp = request.getHeader("X-Real-IP");
        }
        
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        logger.info("Extracted client IP: {}", clientIp);
        return clientIp;
    }

    /**
     * Get location details from IP address using ipapi.co (free service)
     * Returns location in format: "City, Country (Latitude, Longitude)"
     */
    public String getLocationFromIp(String ipAddress) {
        try {
            // Check if IP is localhost/private - use default dev location
            if (isPrivateIp(ipAddress)) {
                return "Development/Local Environment (Testing Location)";
            }

            // Using ipapi.co - free and accurate
            String url = "https://ipapi.co/" + ipAddress + "/json/";
            String response = restTemplate.getForObject(url, String.class);
            
            JsonNode jsonNode = objectMapper.readTree(response);
            String city = jsonNode.get("city").asText("City");
            String region = jsonNode.get("region").asText("");
            String country = jsonNode.get("country_name").asText("Country");
            String latitude = jsonNode.get("latitude").asText("");
            String longitude = jsonNode.get("longitude").asText("");
            
            // Build location string with coordinates
            String regionStr = region.isEmpty() ? "" : ", " + region;
            String coordStr = "";
            if (!latitude.isEmpty() && !longitude.isEmpty()) {
                try {
                    double lat = Double.parseDouble(latitude);
                    double lon = Double.parseDouble(longitude);
                    coordStr = String.format(" (%.4f, %.4f)", lat, lon);
                } catch (NumberFormatException e) {
                    coordStr = "";
                }
            }
            
            String location = String.format("%s%s, %s%s", city, regionStr, country, coordStr);
            
            logger.info("Location resolved for IP {}: {}", ipAddress, location);
            return location;
        } catch (RestClientException e) {
            logger.warn("Failed to fetch location for IP {}: {}", ipAddress, e.getMessage());
            return "Location service unavailable";
        } catch (Exception e) {
            logger.error("Error processing location response for IP {}: {}", ipAddress, e.getMessage());
            return "Location data unavailable";
        }
    }

    /**
     * Check if IP is private/local
     */
    private boolean isPrivateIp(String ip) {
        return ip.startsWith("127.") || 
               ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.") ||
               ip.equals("localhost") ||
               ip.equals("::1");
    }

    public String getLocationFromCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "Location coordinates not provided";
        }

        try {
            // Using Nominatim (OpenStreetMap) - free reverse geocoding
            // Format: https://nominatim.openstreetmap.org/reverse?lat=<lat>&lon=<lon>&format=json
            String url = String.format(
                "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json",
                latitude, longitude
            );
            
            // Set User-Agent header as required by Nominatim usage policy
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "EventConnect-BookingSystem/1.0");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
            );
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode address = jsonNode.get("address");
            
            if (address != null) {
                String city = address.has("city") ? address.get("city").asText() : 
                             address.has("town") ? address.get("town").asText() :
                             address.has("village") ? address.get("village").asText() : "";
                String state = address.has("state") ? address.get("state").asText() : "";
                String country = address.has("country") ? address.get("country").asText() : "";
                
                // Build location string
                StringBuilder location = new StringBuilder();
                if (!city.isEmpty()) location.append(city);
                if (!state.isEmpty()) {
                    if (location.length() > 0) location.append(", ");
                    location.append(state);
                }
                if (!country.isEmpty()) {
                    if (location.length() > 0) location.append(", ");
                    location.append(country);
                }
                
                String locationStr = location.length() > 0 ? location.toString() : "Unknown Location";
                logger.info("Reverse geocoded ({}, {}) to: {}", latitude, longitude, locationStr);
                return locationStr;
            }
            
            return String.format("Lat: %.4f, Lon: %.4f", latitude, longitude);
        } catch (RestClientException e) {
            logger.warn("Failed to reverse geocode coordinates ({}, {}): {}", latitude, longitude, e.getMessage());
            return String.format("Lat: %.4f, Lon: %.4f", latitude, longitude);
        } catch (Exception e) {
            logger.error("Error processing reverse geocoding for ({}, {}): {}", latitude, longitude, e.getMessage());
            return String.format("Lat: %.4f, Lon: %.4f", latitude, longitude);
        }
    }
}
