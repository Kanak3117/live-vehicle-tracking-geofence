package com.kanak.tracking.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-0123456789";

    @Test
    void tokenRoundTripKeepsUserAndRole() {
        JwtService jwt = new JwtService(SECRET, 60);
        var claims = jwt.parse(jwt.generate("admin", "ADMIN"));
        assertEquals("admin", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generate("admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(JwtException.class, () -> jwt.parse(tampered));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1);                // already expired
        assertThrows(JwtException.class, () -> jwt.parse(jwt.generate("admin", "ADMIN")));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService(SECRET, 60).generate("admin", "ADMIN");
        JwtService other = new JwtService("a-completely-different-secret-key-9876543210", 60);
        assertThrows(JwtException.class, () -> other.parse(token));
    }
}