package ru.example.otpcodes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.otpcodes.dao.OtpConfigDao;
import ru.example.otpcodes.domain.OtpConfig;
import ru.example.otpcodes.dto.OtpConfigDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpConfigService {

    private final OtpConfigDao otpConfigDao;

    public OtpConfig get() {
        return otpConfigDao.find();
    }

    @Transactional
    public OtpConfig update(OtpConfigDto dto) {
        OtpConfig config = OtpConfig.builder()
                .codeLength(dto.codeLength())
                .ttlSeconds(dto.ttlSeconds())
                .build();
        otpConfigDao.update(config);
        log.info("Updated OTP config: codeLength={}, ttlSeconds={}", dto.codeLength(), dto.ttlSeconds());
        return config;
    }
}
