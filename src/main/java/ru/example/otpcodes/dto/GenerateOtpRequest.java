package ru.example.otpcodes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.example.otpcodes.domain.Channel;

public record GenerateOtpRequest(
        @Size(max = 128) String operationId,
        @NotNull Channel channel,
        @Size(max = 255) String destination
) {

}
