package ru.example.otpcodes.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.example.otpcodes.domain.Channel;
import ru.example.otpcodes.domain.OtpCode;
import ru.example.otpcodes.domain.OtpStatus;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OtpCodeDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<OtpCode> ROW_MAPPER = (rs, rowNum) -> OtpCode.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .operationId(rs.getString("operation_id"))
            .code(rs.getString("code"))
            .status(OtpStatus.valueOf(rs.getString("status")))
            .channel(Channel.valueOf(rs.getString("channel")))
            .createdAt(rs.getObject("created_at", OffsetDateTime.class))
            .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
            .usedAt(rs.getObject("used_at", OffsetDateTime.class))
            .build();

    public OtpCode save(OtpCode otpCode) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO otp_codes (user_id, operation_id, code, status, channel, expires_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, otpCode.getUserId());
            ps.setString(2, otpCode.getOperationId());
            ps.setString(3, otpCode.getCode());
            ps.setString(4, otpCode.getStatus().name());
            ps.setString(5, otpCode.getChannel().name());
            ps.setTimestamp(6, Timestamp.from(otpCode.getExpiresAt().toInstant()));
            return ps;
        }, keyHolder);
        Number id = (Number) keyHolder.getKeys().get("id");
        otpCode.setId(id.longValue());
        log.debug("Inserted otp id={}, userId={}, channel={}, status={}",
                otpCode.getId(), otpCode.getUserId(), otpCode.getChannel(), otpCode.getStatus());
        return otpCode;
    }

    public Optional<OtpCode> findActiveByUserAndCode(long userId, String code) {
        List<OtpCode> list = jdbcTemplate.query(
                "SELECT id, user_id, operation_id, code, status, channel, created_at, expires_at, used_at " +
                        "FROM otp_codes WHERE user_id = ? AND code = ? AND status = 'ACTIVE'",
                ROW_MAPPER, userId, code);
        return list.stream().findFirst();
    }

    public int markUsed(long otpId) {
        int affected = jdbcTemplate.update(
                "UPDATE otp_codes SET status = 'USED', used_at = ? WHERE id = ? AND status = 'ACTIVE'",
                Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()), otpId);
        log.debug("Marked otp id={} as USED, affected={}", otpId, affected);
        return affected;
    }

    public int expireOverdue() {
        int affected = jdbcTemplate.update(
                "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < NOW()");
        if (affected > 0) {
            log.info("Expired {} overdue OTP codes", affected);
        }
        return affected;
    }

    public int deleteByUserId(long userId) {
        int affected = jdbcTemplate.update("DELETE FROM otp_codes WHERE user_id = ?", userId);
        log.debug("Deleted {} OTP codes for userId={}", affected, userId);
        return affected;
    }
}
