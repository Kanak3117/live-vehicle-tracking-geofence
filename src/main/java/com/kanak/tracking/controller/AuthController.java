package com.kanak.tracking.controller;

import com.kanak.tracking.domain.model.UserAccount;
import com.kanak.tracking.dto.ApiError;
import com.kanak.tracking.repository.UserAccountRepository;
import com.kanak.tracking.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, String role, long expiresInSeconds) {}

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByUsername(request.username()).orElse(null);
        // same message for unknown user and wrong password, so usernames can't be probed
        if (user == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiError(Instant.now(), 401, "Unauthorized", "Invalid username or password"));
        }
        String role = user.getRole().name();
        return ResponseEntity.ok(new LoginResponse(
                jwtService.generate(user.getUsername(), role), role, jwtService.expirySeconds()));
    }
}