package com.kanak.tracking.controller;

import com.kanak.tracking.domain.model.GeofenceEntity;
import com.kanak.tracking.dto.GeofenceRequest;
import com.kanak.tracking.service.GeofenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geofences")
public class GeofenceController {

    private final GeofenceService service;

    public GeofenceController(GeofenceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<GeofenceEntity> create(@Valid @RequestBody GeofenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toEntity()));
    }

    @GetMapping
    public List<GeofenceEntity> list() {
        return service.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}