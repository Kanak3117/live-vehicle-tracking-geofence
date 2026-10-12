package com.kanak.tracking.controller;

import com.kanak.tracking.exception.GlobalExceptionHandler;
import com.kanak.tracking.service.PingIngestionService;
import com.kanak.tracking.service.PingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingControllerTest {

    private static final String VALID = """
            {"deviceId":"D1","lat":28.6139,"lon":77.2090,"bearing":90,"speed":30,
             "timestamp":"2026-10-10T06:30:00Z"}""";

    private PingIngestionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PingIngestionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void serviceReturns(PingResult result) {
        when(service.ingest(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(Instant.class)))
                .thenReturn(result);
    }

    @Test
    void acceptedPingReturns200() throws Exception {
        serviceReturns(PingResult.ACCEPTED);
        mvc.perform(post("/api/pings").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void unknownDeviceReturns404() throws Exception {
        serviceReturns(PingResult.UNKNOWN_DEVICE);
        mvc.perform(post("/api/pings").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("UNKNOWN_DEVICE"));
    }

    @Test
    void latitudeOutOfRangeReturns400() throws Exception {
        String bad = VALID.replace("28.6139", "95");
        mvc.perform(post("/api/pings").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingDeviceIdReturns400() throws Exception {
        String bad = VALID.replace("\"deviceId\":\"D1\",", "");
        mvc.perform(post("/api/pings").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }
}