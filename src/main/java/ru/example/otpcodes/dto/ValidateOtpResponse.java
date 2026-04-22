package ru.example.otpcodes.dto;

public record ValidateOtpResponse(boolean valid, String status, String message) {

}
