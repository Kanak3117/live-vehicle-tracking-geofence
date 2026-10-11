package com.kanak.tracking.service;

import com.kanak.tracking.domain.geofence.Geofence;
import com.kanak.tracking.domain.model.GeofenceEntity;
import com.kanak.tracking.index.SpatialGridIndex;
import com.kanak.tracking.repository.GeofenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceService.class);

    private final GeofenceRepository repository;
    private final GeofenceMapper mapper;
    private final SpatialGridIndex index;

    public GeofenceService(GeofenceRepository repository, GeofenceMapper mapper, SpatialGridIndex index) {
        this.repository = repository;
        this.mapper = mapper;
        this.index = index;
    }

    /** Rebuilds the in-memory index from the DB whenever the app starts. */
    @EventListener(ApplicationReadyEvent.class)
    public void loadIndexOnStartup() {
        index.clear();
        List<GeofenceEntity> all = repository.findAll();
        all.forEach(e -> index.add(mapper.toDomain(e)));
        log.info("geofence_index_loaded count={}", all.size());
    }

    @Transactional
    public GeofenceEntity create(GeofenceEntity entity) {
        mapper.toDomain(entity);                         // validates before anything is saved
        GeofenceEntity saved = repository.save(entity);
        index.add(mapper.toDomain(saved));               // DB and index stay in step
        log.info("geofence_created id={} type={}", saved.getId(), saved.getType());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Geofence not found: " + id);
        }
        Geofence g = index.findById(id);
        if (g != null) {
            index.remove(g);
        }
        repository.deleteById(id);
        log.info("geofence_deleted id={}", id);
    }

    public List<GeofenceEntity> findAll() {
        return repository.findAll();
    }
}