package ru.example.otpcodes.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.otpcodes.dto.GenerateOtpResponse;
import ru.example.otpcodes.dto.ValidateOtpResponse;
import ru.example.otpcodes.domain.Role;
import ru.example.otpcodes.security.AuthenticatedUser;
import ru.example.otpcodes.security.JwtAuthenticationFilter;
import ru.example.otpcodes.security.JwtService;
import ru.example.otpcodes.security.SecurityConfig;
import ru.example.otpcodes.service.OtpService;

@WebMvcTest(UserOtpController.class)
@Import({SecurityAutoConfiguration.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-that-is-at-least-32-bytes-long!",
        "security.jwt.ttl-minutes=60"
})
class UserOtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OtpService otpService;
    @MockitoBean
    private JwtService jwtService;

    private static UsernamePasswordAuthenticationToken userAuth() {
        var principal = new AuthenticatedUser(7L, "mr credo", Role.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void generate_returns401_withoutAuth() throws Exception {
        mockMvc.perform(post("/otp/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"FILE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generate_returns403_forAdmin() throws Exception {
        mockMvc.perform(post("/otp/generate")
                        .with(user("a").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"FILE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void generate_returns200_forAuthenticatedUser() throws Exception {
        when(otpService.generate(anyLong(), any())).thenReturn(
                new GenerateOtpResponse(100L, "tx-1", OffsetDateTime.now().plusMinutes(5))
        );

        mockMvc.perform(post("/otp/generate")
                        .with(csrf())
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operationId":"tx-1","channel":"FILE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpId").value(100))
                .andExpect(jsonPath("$.operationId").value("tx-1"));
    }

    @Test
    void validate_returns200_withResultPayload() throws Exception {
        when(otpService.validate(anyLong(), any())).thenReturn(
                new ValidateOtpResponse(true, "USED", "Code accepted")
        );

        mockMvc.perform(post("/otp/validate")
                        .with(csrf())
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"123456","operationId":"tx-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("USED"));
    }
}
