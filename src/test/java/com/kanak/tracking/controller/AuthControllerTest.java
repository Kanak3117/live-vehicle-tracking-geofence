package com.kanak.tracking.controller;

import com.kanak.tracking.domain.model.Role;
import com.kanak.tracking.domain.model.UserAccount;
import com.kanak.tracking.repository.UserAccountRepository;
import com.kanak.tracking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode("admin123"));
        admin.setRole(Role.ADMIN);

        UserAccountRepository repo = mock(UserAccountRepository.class);
        when(repo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.findByUsername("ghost")).thenReturn(Optional.empty());

        JwtService jwt = new JwtService("unit-test-secret-that-is-long-enough-0123456789", 60);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(repo, encoder, jwt)).build();
    }

    private String body(String user, String pass) {
        return "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}";
    }

    @Test
    void validLoginReturnsTokenAndRole() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("admin", "nope")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void unknownUserGetsSameMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("ghost", "x")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}