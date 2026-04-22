package ru.example.otpcodes.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.otpcodes.dto.TokenResponse;
import ru.example.otpcodes.exception.BusinessException;
import ru.example.otpcodes.security.JwtAuthenticationFilter;
import ru.example.otpcodes.security.JwtService;
import ru.example.otpcodes.security.SecurityConfig;
import ru.example.otpcodes.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({SecurityAutoConfiguration.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-that-is-at-least-32-bytes-long!",
        "security.jwt.ttl-minutes=60"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void register_returns201_onSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"mr credo","password":"secret123","role":"USER"}
                                """))
                .andExpect(status().isCreated());

        verify(authService).register(any());
    }

    @Test
    void register_returns400_onInvalidInput() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"","password":"x","role":"GUEST"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409_onDuplicate() throws Exception {
        doThrow(new BusinessException(HttpStatus.CONFLICT, "Login already taken"))
                .when(authService).register(any());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"mr credo","password":"secret123","role":"USER"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returns200AndToken() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponse.bearer("jwt", 3600));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"mr credo","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
