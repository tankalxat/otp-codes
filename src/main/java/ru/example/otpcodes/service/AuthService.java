package ru.example.otpcodes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.otpcodes.dao.UserDao;
import ru.example.otpcodes.domain.Role;
import ru.example.otpcodes.domain.User;
import ru.example.otpcodes.dto.LoginRequest;
import ru.example.otpcodes.dto.RegisterRequest;
import ru.example.otpcodes.dto.TokenResponse;
import ru.example.otpcodes.exception.BusinessException;
import ru.example.otpcodes.security.JwtService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        Role role = Role.valueOf(request.role());
        if (userDao.findByLogin(request.login()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Login already taken");
        }
        if (role == Role.ADMIN && userDao.adminExists()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Administrator already exists");
        }
        User user = User.builder()
                .login(request.login())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .build();
        userDao.save(user);
        log.info("Registered user login={}, role={}", user.getLogin(), user.getRole());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userDao.findByLogin(request.login())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtService.generate(user);
        log.info("Issued token for login={}, role={}", user.getLogin(), user.getRole());
        return TokenResponse.bearer(token, jwtService.ttlSeconds());
    }
}
