package ru.example.otpcodes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.example.otpcodes.dao.OtpCodeDao;
import ru.example.otpcodes.domain.Channel;
import ru.example.otpcodes.domain.OtpCode;
import ru.example.otpcodes.domain.OtpConfig;
import ru.example.otpcodes.domain.OtpStatus;
import ru.example.otpcodes.dto.GenerateOtpRequest;
import ru.example.otpcodes.dto.GenerateOtpResponse;
import ru.example.otpcodes.dto.ValidateOtpRequest;
import ru.example.otpcodes.dto.ValidateOtpResponse;
import ru.example.otpcodes.exception.BusinessException;
import ru.example.otpcodes.sender.NotificationRouter;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpCodeDao otpCodeDao;
    @Mock
    private OtpConfigService otpConfigService;
    @Mock
    private NotificationRouter notificationRouter;

    @InjectMocks
    private OtpService otpService;

    @Test
    void generate_savesActiveOtpAndRoutesToChannel() {
        when(otpConfigService.get()).thenReturn(OtpConfig.builder().codeLength(6).ttlSeconds(300).build());
        doAnswer(inv -> {
            OtpCode otp = inv.getArgument(0);
            otp.setId(42L);
            return otp;
        }).when(otpCodeDao).save(any(OtpCode.class));

        GenerateOtpResponse response = otpService.generate(
                1L, new GenerateOtpRequest("tx-1", Channel.FILE, null));

        assertThat(response.otpId()).isEqualTo(42L);
        assertThat(response.operationId()).isEqualTo("tx-1");
        assertThat(response.expiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        verify(notificationRouter).route(eq(Channel.FILE), eq(null), anyString());
    }

    @Test
    void generate_wrapsSenderFailureInBusinessException() {
        when(otpConfigService.get()).thenReturn(OtpConfig.builder().codeLength(6).ttlSeconds(300).build());
        when(otpCodeDao.save(any(OtpCode.class))).thenAnswer(inv -> {
            OtpCode otp = inv.getArgument(0);
            otp.setId(1L);
            return otp;
        });
        doThrow(new RuntimeException("smtp down"))
                .when(notificationRouter).route(any(), any(), anyString());

        assertThatThrownBy(() -> otpService.generate(
                1L, new GenerateOtpRequest("tx-1", Channel.EMAIL, "a@b.c")))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void validate_returnsInvalid_whenCodeNotFound() {
        when(otpCodeDao.findActiveByUserAndCode(1L, "111111")).thenReturn(Optional.empty());

        ValidateOtpResponse response = otpService.validate(1L, new ValidateOtpRequest("111111", null));

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo("INVALID");
        verify(otpCodeDao, never()).markUsed(anyLong());
    }

    @Test
    void validate_returnsExpired_whenPastExpiry() {
        OtpCode otp = OtpCode.builder()
                .id(1L).userId(1L).code("111111").status(OtpStatus.ACTIVE)
                .channel(Channel.FILE)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .build();
        when(otpCodeDao.findActiveByUserAndCode(1L, "111111")).thenReturn(Optional.of(otp));

        ValidateOtpResponse response = otpService.validate(1L, new ValidateOtpRequest("111111", null));

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo("EXPIRED");
        verify(otpCodeDao, never()).markUsed(anyLong());
    }

    @Test
    void validate_returnsInvalid_onOperationIdMismatch() {
        OtpCode otp = OtpCode.builder()
                .id(1L).userId(1L).code("111111").operationId("tx-1").status(OtpStatus.ACTIVE)
                .channel(Channel.FILE)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5))
                .build();
        when(otpCodeDao.findActiveByUserAndCode(1L, "111111")).thenReturn(Optional.of(otp));

        ValidateOtpResponse response = otpService.validate(1L, new ValidateOtpRequest("111111", "tx-other"));

        assertThat(response.valid()).isFalse();
        assertThat(response.status()).isEqualTo("INVALID");
    }

    @Test
    void validate_marksUsed_onMatch() {
        OtpCode otp = OtpCode.builder()
                .id(1L).userId(1L).code("111111").operationId("tx-1").status(OtpStatus.ACTIVE)
                .channel(Channel.FILE)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5))
                .build();
        when(otpCodeDao.findActiveByUserAndCode(1L, "111111")).thenReturn(Optional.of(otp));
        when(otpCodeDao.markUsed(1L)).thenReturn(1);

        ValidateOtpResponse response = otpService.validate(1L, new ValidateOtpRequest("111111", "tx-1"));

        assertThat(response.valid()).isTrue();
        assertThat(response.status()).isEqualTo("USED");
        verify(otpCodeDao).markUsed(1L);
    }
}
