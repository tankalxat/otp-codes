package ru.example.otpcodes.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.example.otpcodes.dto.GenerateOtpRequest;
import ru.example.otpcodes.dto.GenerateOtpResponse;
import ru.example.otpcodes.dto.ValidateOtpRequest;
import ru.example.otpcodes.dto.ValidateOtpResponse;
import ru.example.otpcodes.security.AuthenticatedUser;
import ru.example.otpcodes.service.OtpService;

@Slf4j
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class UserOtpController {

    private final OtpService otpService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateOtpResponse> generate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody GenerateOtpRequest request) {
        log.info("POST /otp/generate userId={}, channel={}, operationId={}",
                principal.id(), request.channel(), request.operationId());
        return ResponseEntity.ok(otpService.generate(principal.id(), request));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateOtpResponse> validate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ValidateOtpRequest request) {
        log.info("POST /otp/validate userId={}, operationId={}", principal.id(), request.operationId());
        return ResponseEntity.ok(otpService.validate(principal.id(), request));
    }
}
