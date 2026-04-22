package ru.example.otpcodes.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {

    private Long id;
    private Long userId;
    private String operationId;
    private String code;
    private OtpStatus status;
    private Channel channel;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;
}
