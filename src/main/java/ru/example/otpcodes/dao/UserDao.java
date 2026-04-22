package ru.example.otpcodes.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.example.otpcodes.domain.Role;
import ru.example.otpcodes.domain.User;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .login(rs.getString("login"))
            .passwordHash(rs.getString("password_hash"))
            .role(Role.valueOf(rs.getString("role")))
            .createdAt(rs.getObject("created_at", OffsetDateTime.class))
            .build();

    public void save(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            return ps;
        }, keyHolder);
        Number id = (Number) keyHolder.getKeys().get("id");
        user.setId(id.longValue());
        log.debug("Inserted user id={}, login={}, role={}", user.getId(), user.getLogin(), user.getRole());
    }

    public Optional<User> findByLogin(String login) {
        List<User> list = jdbcTemplate.query(
                "SELECT id, login, password_hash, role, created_at FROM users WHERE login = ?",
                ROW_MAPPER, login);
        return list.stream().findFirst();
    }

    public Optional<User> findById(long id) {
        List<User> list = jdbcTemplate.query(
                "SELECT id, login, password_hash, role, created_at FROM users WHERE id = ?",
                ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    public boolean adminExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class);
        return count != null && count > 0;
    }

    public List<User> findAllNonAdmins() {
        return jdbcTemplate.query(
                "SELECT id, login, password_hash, role, created_at FROM users WHERE role <> 'ADMIN' ORDER BY id",
                ROW_MAPPER);
    }

    public void deleteById(long id) {
        int affected = jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        log.debug("Deleted user id={}, affected={}", id, affected);
    }
}
