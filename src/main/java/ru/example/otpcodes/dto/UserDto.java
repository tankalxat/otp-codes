package ru.example.otpcodes.dto;

import java.time.OffsetDateTime;

public record UserDto(Long id, String login, String role, OffsetDateTime createdAt) {

}
