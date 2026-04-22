package ru.example.otpcodes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.otpcodes.dao.OtpCodeDao;
import ru.example.otpcodes.domain.OtpCode;
import ru.example.otpcodes.domain.OtpConfig;
import ru.example.otpcodes.domain.OtpStatus;
import ru.example.otpcodes.dto.GenerateOtpRequest;
import ru.example.otpcodes.dto.GenerateOtpResponse;
import ru.example.otpcodes.dto.ValidateOtpRequest;
import ru.example.otpcodes.dto.ValidateOtpResponse;
import ru.example.otpcodes.exception.BusinessException;
import ru.example.otpcodes.sender.NotificationRouter;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigService otpConfigService;
    private final NotificationRouter notificationRouter;

    @Transactional
    public GenerateOtpResponse generate(long userId, GenerateOtpRequest request) {
        OtpConfig config = otpConfigService.get();
        String code = generateNumericCode(config.getCodeLength());
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(config.getTtlSeconds());

        OtpCode otp = OtpCode.builder()
                .userId(userId)
                .operationId(request.operationId())
                .code(code)
                .status(OtpStatus.ACTIVE)
                .channel(request.channel())
                .expiresAt(expiresAt)
                .build();
        otp = otpCodeDao.save(otp);

        String destination = request.destination();
        try {
            notificationRouter.route(request.channel(), destination, code);
        } catch (RuntimeException ex) {
            log.error("Failed to deliver OTP id={} via {}: {}", otp.getId(), request.channel(), ex.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "Failed to deliver OTP via " + request.channel() + ": " + ex.getMessage());
        }

        log.info("Generated OTP id={} for userId={}, channel={}, operationId={}",
                otp.getId(), userId, request.channel(), request.operationId());
        return new GenerateOtpResponse(otp.getId(), otp.getOperationId(), expiresAt);
    }

    @Transactional
    public ValidateOtpResponse validate(long userId, ValidateOtpRequest request) {
        var maybeOtp = otpCodeDao.findActiveByUserAndCode(userId, request.code());
        if (maybeOtp.isEmpty()) {
            log.info("OTP validation failed for userId={}: not found or not active", userId);
            return new ValidateOtpResponse(false, "INVALID", "Code not found or not active");
        }
        OtpCode otp = maybeOtp.get();
        if (otp.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            log.info("OTP validation failed for userId={}: expired (otpId={})", userId, otp.getId());
            return new ValidateOtpResponse(false, "EXPIRED", "Code expired");
        }
        if (request.operationId() != null && !request.operationId().equals(otp.getOperationId())) {
            log.info("OTP validation failed for userId={}: operationId mismatch", userId);
            return new ValidateOtpResponse(false, "INVALID", "Operation id mismatch");
        }
        int affected = otpCodeDao.markUsed(otp.getId());
        if (affected == 0) {
            return new ValidateOtpResponse(false, "INVALID", "Code already used");
        }
        log.info("OTP validated successfully userId={}, otpId={}", userId, otp.getId());
        return new ValidateOtpResponse(true, "USED", "Code accepted");
    }

    private static String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
