package ru.example.otpcodes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.otpcodes.dao.OtpCodeDao;
import ru.example.otpcodes.dao.UserDao;
import ru.example.otpcodes.domain.Role;
import ru.example.otpcodes.domain.User;
import ru.example.otpcodes.dto.UserDto;
import ru.example.otpcodes.exception.BusinessException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final OtpCodeDao otpCodeDao;

    public List<UserDto> listNonAdmins() {
        return userDao.findAllNonAdmins().stream()
                .map(u -> new UserDto(u.getId(), u.getLogin(), u.getRole().name(), u.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void delete(long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cannot delete administrator");
        }
        otpCodeDao.deleteByUserId(userId);
        userDao.deleteById(userId);
        log.info("Deleted user id={}, login={}", userId, user.getLogin());
    }
}
