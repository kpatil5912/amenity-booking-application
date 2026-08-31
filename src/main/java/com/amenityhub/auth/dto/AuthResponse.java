package com.amenityhub.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String email,
        List<String> roles) {

    public static AuthResponse bearer(String accessToken, String email, List<String> roles) {
        return new AuthResponse(accessToken, "Bearer", email, roles);
    }
}
