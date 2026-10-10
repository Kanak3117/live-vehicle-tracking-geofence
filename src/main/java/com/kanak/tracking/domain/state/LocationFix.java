package com.kanak.tracking.domain.state;

import java.time.Instant;

public record LocationFix(long vehicleId, double lat, double lon,
                          double bearing, double speed, Instant timestamp) {}