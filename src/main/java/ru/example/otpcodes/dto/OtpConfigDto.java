package ru.example.otpcodes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record OtpConfigDto(
        @Min(4) @Max(10) int codeLength,
        @Min(30) @Max(86_400) int ttlSeconds
) {

}
