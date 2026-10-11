package com.dlog.auth.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, Instant expiresAt) {
}
