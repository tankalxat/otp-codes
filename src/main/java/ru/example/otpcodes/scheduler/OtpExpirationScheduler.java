package ru.example.otpcodes.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.example.otpcodes.dao.OtpCodeDao;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpExpirationScheduler {

    private final OtpCodeDao otpCodeDao;

    @Scheduled(fixedRateString = "${otp.expiration.scan-interval-ms}")
    public void expireOverdueCodes() {
        log.debug("Running OTP expiration scan");
        otpCodeDao.expireOverdue();
    }
}
