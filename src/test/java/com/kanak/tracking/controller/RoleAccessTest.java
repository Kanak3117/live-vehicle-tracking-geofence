package com.kanak.tracking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks role-based access end to end: real login, real JWT, real security rules.
 * Uses the demo users created by UserSeeder.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAccessTest {

    private static final String EVENTS_URL =
            "/api/geofence-events?vehicleId=1&from=2026-10-12T00:00:00Z&to=2026-10-12T23:59:59Z";

    @Autowired
    private MockMvc mvc;

    private String tokenFor(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("No token in login response");
        }
        return m.group(1);
    }

    @Test
    void approverCanReadGeofenceEvents() throws Exception {
        String token = tokenFor("approver", "approver123");
        mvc.perform(get(EVENTS_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void approverCannotListGeofences() throws Exception {
        String token = tokenFor("approver", "approver123");
        mvc.perform(get("/api/geofences").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void approverCannotReadVehicleEta() throws Exception {
        String token = tokenFor("approver", "approver123");
        mvc.perform(get("/api/vehicles/1/eta").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotReadGeofenceEvents() throws Exception {
        String token = tokenFor("employee", "employee123");
        mvc.perform(get(EVENTS_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadGeofenceEvents() throws Exception {
        String token = tokenFor("admin", "admin123");
        mvc.perform(get(EVENTS_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void noTokenGetsUnauthorized() throws Exception {
        mvc.perform(get(EVENTS_URL))
                .andExpect(status().isUnauthorized());
    }
}