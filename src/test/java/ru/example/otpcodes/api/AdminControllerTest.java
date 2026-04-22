package ru.example.otpcodes.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.otpcodes.domain.OtpConfig;
import ru.example.otpcodes.dto.UserDto;
import ru.example.otpcodes.security.JwtAuthenticationFilter;
import ru.example.otpcodes.security.JwtService;
import ru.example.otpcodes.security.SecurityConfig;
import ru.example.otpcodes.service.OtpConfigService;
import ru.example.otpcodes.service.UserService;

@WebMvcTest(AdminController.class)
@Import({SecurityAutoConfiguration.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-that-is-at-least-32-bytes-long!",
        "security.jwt.ttl-minutes=60"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private OtpConfigService otpConfigService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void listUsers_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_returns403_forNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("u").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_returns200_forAdmin() throws Exception {
        when(userService.listNonAdmins()).thenReturn(List.of(new UserDto(1L, "mr credo", "USER", null)));

        mockMvc.perform(get("/admin/users").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("mr credo"));
    }

    @Test
    void deleteUser_returns204_forAdmin() throws Exception {
        mockMvc.perform(delete("/admin/users/{id}", 42L)
                        .with(user("a").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).delete(42L);
    }

    @Test
    void updateConfig_returns200_forAdmin() throws Exception {
        when(otpConfigService.update(any())).thenReturn(OtpConfig.builder().codeLength(8).ttlSeconds(600).build());

        mockMvc.perform(put("/admin/otp-config")
                        .with(user("a").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codeLength": 8, "ttlSeconds": 600}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeLength").value(8))
                .andExpect(jsonPath("$.ttlSeconds").value(600));
    }

    @Test
    void updateConfig_returns403_forUser() throws Exception {
        mockMvc.perform(put("/admin/otp-config")
                        .with(user("u").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeLength\":6,\"ttlSeconds\":300}"))
                .andExpect(status().isForbidden());
    }
}
