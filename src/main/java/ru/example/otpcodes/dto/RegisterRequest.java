package ru.example.otpcodes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String login,
        @NotBlank @Size(min = 6, max = 128) String password,
        @NotBlank @Pattern(regexp = "ADMIN|USER") String role
) {

}
