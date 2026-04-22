package ru.example.otpcodes.security;

import ru.example.otpcodes.domain.Role;

public record AuthenticatedUser(Long id, String login, Role role) {

}
