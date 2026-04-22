package ru.example.otpcodes.dto;

import java.time.OffsetDateTime;

public record GenerateOtpResponse(Long otpId, String operationId, OffsetDateTime expiresAt) {

}
