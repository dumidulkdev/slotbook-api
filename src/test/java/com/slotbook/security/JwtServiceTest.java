package com.slotbook.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotbook.config.AppProperties;
import com.slotbook.domain.Role;
import com.slotbook.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("unit-test-jwt-secret-key-32-characters-min");
        properties.getJwt().setExpirationMinutes(30);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatesAndValidatesToken() throws Exception {
        UserEntity user = new UserEntity("Test User", "user@test.dev", "hash", Role.CLIENT);
        var idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 99L);
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.dev");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.CLIENT);
    }
}