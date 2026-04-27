package org.example.securitytokenservice.dto;

import java.time.Instant;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(String login, String password, String role) {
    }

    public record LoginRequest(String login, String password) {
    }

    public record UpdateOtpConfigRequest(long ttlSeconds, int codeLength) {
    }

    public record GenerateOtpRequest(String operationId, String destination, String channel) {
    }

    public record ValidateOtpRequest(String operationId, String code) {
    }

    public record TokenResponse(String token, String tokenType, String role, Instant expiresAt) {
    }

    public record UserResponse(long id, String login, String role, Instant createdAt) {
    }

    public record OtpConfigResponse(long ttlSeconds, int codeLength, Instant updatedAt) {
    }

    public record MessageResponse(String message) {
    }

    public record ErrorResponse(String error) {
    }
}
