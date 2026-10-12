package com.kanak.tracking.controller;

import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.exception.GlobalExceptionHandler;
import com.kanak.tracking.service.EtaService;
import com.kanak.tracking.service.LiveLocationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleControllerTest {

    private LiveLocationStore store;
    private EtaService etaService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        store = new LiveLocationStore();
        etaService = mock(EtaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new VehicleController(store, etaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void latestLocationIsReturned() throws Exception {
        store.offer(new LocationFix(7L, 28.6139, 77.2090, 0, 10, Instant.parse("2026-10-10T06:30:00Z")));
        mvc.perform(get("/api/vehicles/7/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(28.6139));
    }

    @Test
    void unknownVehicleLocationReturns404() throws Exception {
        mvc.perform(get("/api/vehicles/99/location"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No live location yet for vehicle 99"));
    }

    @Test
    void etaWithoutLocationReturns404() throws Exception {
        when(etaService.getEta(9L)).thenThrow(new NoSuchElementException("No live location yet for vehicle 9"));
        mvc.perform(get("/api/vehicles/9/eta"))
                .andExpect(status().isNotFound());
    }
}