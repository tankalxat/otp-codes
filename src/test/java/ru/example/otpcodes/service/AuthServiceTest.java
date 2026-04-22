package ru.example.otpcodes.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.example.otpcodes.dao.UserDao;
import ru.example.otpcodes.domain.Role;
import ru.example.otpcodes.domain.User;
import ru.example.otpcodes.dto.LoginRequest;
import ru.example.otpcodes.dto.RegisterRequest;
import ru.example.otpcodes.dto.TokenResponse;
import ru.example.otpcodes.exception.BusinessException;
import ru.example.otpcodes.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_savesUser_whenLoginFreeAndNoAdminExists() {
        RegisterRequest req = new RegisterRequest("mr credo", "secret123", "USER");
        when(userDao.findByLogin("mr credo")).thenReturn(Optional.empty());
        doReturn("hash").when(passwordEncoder).encode("secret123");

        authService.register(req);

        verify(userDao).save(any(User.class));
    }

    @Test
    void register_rejectsDuplicateLogin() {
        RegisterRequest req = new RegisterRequest("mr credo", "secret123", "USER");
        when(userDao.findByLogin("mr credo"))
                .thenReturn(Optional.of(User.builder().login("mr credo").role(Role.USER).build()));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
        verify(userDao, never()).save(any());
    }

    @Test
    void register_rejectsSecondAdmin() {
        RegisterRequest req = new RegisterRequest("admin2", "secret123", "ADMIN");
        when(userDao.findByLogin("admin2")).thenReturn(Optional.empty());
        when(userDao.adminExists()).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
        verify(userDao, never()).save(any());
    }

    @Test
    void login_returnsToken_onValidCredentials() {
        User user = User.builder().id(1L).login("mr credo").passwordHash("hash").role(Role.USER).build();
        when(userDao.findByLogin("mr credo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generate(user)).thenReturn("jwt-token");
        when(jwtService.ttlSeconds()).thenReturn(3600L);

        TokenResponse response = authService.login(new LoginRequest("mr credo", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = User.builder().id(1L).login("mr credo").passwordHash("hash").role(Role.USER).build();
        when(userDao.findByLogin("mr credo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("mr credo", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_rejectsUnknownUser() {
        when(userDao.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "any")))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
