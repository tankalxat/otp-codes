package ru.example.otpcodes.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.example.otpcodes.domain.OtpConfig;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OtpConfigDao {

    private final JdbcTemplate jdbcTemplate;

    public OtpConfig find() {
        return jdbcTemplate.queryForObject(
                "SELECT code_length, ttl_seconds FROM otp_config WHERE id = 1",
                (rs, rowNum) -> OtpConfig.builder()
                        .codeLength(rs.getInt("code_length"))
                        .ttlSeconds(rs.getInt("ttl_seconds"))
                        .build());
    }

    public void update(OtpConfig config) {
        int affected = jdbcTemplate.update(
                "UPDATE otp_config SET code_length = ?, ttl_seconds = ? WHERE id = 1",
                config.getCodeLength(), config.getTtlSeconds());
        log.debug("Updated otp_config: codeLength={}, ttlSeconds={}, affected={}",
                config.getCodeLength(), config.getTtlSeconds(), affected);
    }
}
