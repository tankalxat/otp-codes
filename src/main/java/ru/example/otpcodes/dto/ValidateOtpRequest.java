package ru.example.otpcodes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidateOtpRequest(
        @NotBlank @Size(min = 4, max = 16) String code,
        @Size(max = 128) String operationId
) {

}
