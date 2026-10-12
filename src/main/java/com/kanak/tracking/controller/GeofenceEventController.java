package com.kanak.tracking.controller;

import com.kanak.tracking.domain.model.GeofenceEvent;
import com.kanak.tracking.service.GeofenceEventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/geofence-events")
public class GeofenceEventController {

    private final GeofenceEventService service;

    public GeofenceEventController(GeofenceEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<GeofenceEvent> events(
            @RequestParam Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.find(vehicleId, from, to);
    }
}