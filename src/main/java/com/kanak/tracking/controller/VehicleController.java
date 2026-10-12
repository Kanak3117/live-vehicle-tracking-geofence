package com.kanak.tracking.controller;

import com.kanak.tracking.domain.state.LocationFix;
import com.kanak.tracking.service.EtaService;
import com.kanak.tracking.service.LiveLocationStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final LiveLocationStore store;
    private final EtaService etaService;

    public VehicleController(LiveLocationStore store, EtaService etaService) {
        this.store = store;
        this.etaService = etaService;
    }

    @GetMapping("/{id}/location")
    public LocationFix location(@PathVariable long id) {
        return store.get(id)
                .orElseThrow(() -> new NoSuchElementException("No live location yet for vehicle " + id));
    }

    @GetMapping("/{id}/eta")
    public EtaService.VehicleEta eta(@PathVariable long id) {
        return etaService.getEta(id);
    }
}