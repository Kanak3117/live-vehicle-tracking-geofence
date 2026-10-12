package com.kanak.tracking.controller;

import com.kanak.tracking.dto.PingRequest;
import com.kanak.tracking.dto.PingResponse;
import com.kanak.tracking.service.PingIngestionService;
import com.kanak.tracking.service.PingResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pings")
public class PingController {

    private final PingIngestionService service;

    public PingController(PingIngestionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PingResponse> ingest(@Valid @RequestBody PingRequest r) {
        PingResult result = service.ingest(r.deviceId(), r.lat(), r.lon(),
                r.bearing() == null ? 0.0 : r.bearing(),
                r.speed() == null ? 0.0 : r.speed(),
                r.timestamp());
        HttpStatus status = (result == PingResult.UNKNOWN_DEVICE) ? HttpStatus.NOT_FOUND : HttpStatus.OK;
        return ResponseEntity.status(status).body(new PingResponse(result));
    }
}