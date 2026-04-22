package ru.example.otpcodes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.example.otpcodes.dao.OtpCodeDao;
import ru.example.otpcodes.dao.UserDao;
import ru.example.otpcodes.domain.Role;
import ru.example.otpcodes.domain.User;
import ru.example.otpcodes.dto.UserDto;
import ru.example.otpcodes.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private OtpCodeDao otpCodeDao;

    @InjectMocks
    private UserService userService;

    @Test
    void listNonAdmins_mapsDomainToDto() {
        User user = User.builder()
                .id(10L).login("mr credo").passwordHash("hash").role(Role.USER)
                .createdAt(OffsetDateTime.now())
                .build();
        when(userDao.findAllNonAdmins()).thenReturn(List.of(user));

        List<UserDto> result = userService.listNonAdmins();

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(10L);
            assertThat(dto.login()).isEqualTo("mr credo");
            assertThat(dto.role()).isEqualTo("USER");
        });
    }

    @Test
    void delete_cascadesOtpCodes() {
        User user = User.builder().id(5L).login("mr credo").role(Role.USER).build();
        when(userDao.findById(5L)).thenReturn(Optional.of(user));

        userService.delete(5L);

        verify(otpCodeDao).deleteByUserId(5L);
        verify(userDao).deleteById(5L);
    }

    @Test
    void delete_rejectsAdmin() {
        User admin = User.builder().id(1L).login("admin").role(Role.ADMIN).build();
        when(userDao.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(otpCodeDao, never()).deleteByUserId(1L);
        verify(userDao, never()).deleteById(1L);
    }

    @Test
    void delete_rejectsUnknownUser() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
