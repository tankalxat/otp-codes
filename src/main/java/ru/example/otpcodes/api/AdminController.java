package ru.example.otpcodes.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.example.otpcodes.domain.OtpConfig;
import ru.example.otpcodes.dto.OtpConfigDto;
import ru.example.otpcodes.dto.UserDto;
import ru.example.otpcodes.service.OtpConfigService;
import ru.example.otpcodes.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final OtpConfigService otpConfigService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        log.info("GET /admin/users");
        return ResponseEntity.ok(userService.listNonAdmins());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id) {
        log.info("DELETE /admin/users/{}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/otp-config")
    public ResponseEntity<OtpConfig> getConfig() {
        log.info("GET /admin/otp-config");
        return ResponseEntity.ok(otpConfigService.get());
    }

    @PutMapping("/otp-config")
    public ResponseEntity<OtpConfig> updateConfig(@Valid @RequestBody OtpConfigDto dto) {
        log.info("PUT /admin/otp-config codeLength={}, ttlSeconds={}", dto.codeLength(), dto.ttlSeconds());
        return ResponseEntity.ok(otpConfigService.update(dto));
    }
}
