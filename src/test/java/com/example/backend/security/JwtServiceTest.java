package com.example.backend.security;

import com.example.backend.model.User;
import com.example.backend.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "dGhpc2lzYXRlc3RzZWNyZXRrZXlmb3JqdW5pdHRlc3Rpbmdvbmx5MTIz";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    private User testUser() {
        return User.builder()
                .id("user-abc-123")
                .username("raj")
                .email("raj@example.com")
                .password("irrelevant-already-hashed")
                .build();
    }

    @Test
    void generatesATokenAndExtractsTheSameUserIdBackOut() {
        String token = jwtService.generateToken(testUser());

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-abc-123");
    }

    @Test
    void twoTokensForTheSameUserAreNotIdentical() {
        String tokenA = jwtService.generateToken(testUser());
        String tokenB = jwtService.generateToken(testUser());

        assertThat(tokenA).isNotBlank();
        assertThat(tokenB).isNotBlank();
    }

    @Test
    void extractUserIdThrowsOnAGarbageToken() {
        assertThatThrownBy(() -> jwtService.extractUserId("not.a.valid.jwt"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void extractUserIdThrowsOnATokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secretKey",
                "YW5vdGhlcmNvbXBsZXRlbHlkaWZmZXJlbnRzZWNyZXRrZXlmb3J0ZXN0aW5n");
        ReflectionTestUtils.setField(otherService, "expirationMs", 3_600_000L);

        String tokenSignedByOther = otherService.generateToken(testUser());

        assertThatThrownBy(() -> jwtService.extractUserId(tokenSignedByOther))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void extractUserIdThrowsOnAnExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String expiredToken = jwtService.generateToken(testUser());

        assertThatThrownBy(() -> jwtService.extractUserId(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
