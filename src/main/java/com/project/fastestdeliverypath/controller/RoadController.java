package com.project.fastestdeliverypath.controller;

import com.project.fastestdeliverypath.dto.RoadDTO;
import com.project.fastestdeliverypath.dto.RoadRequest;
import com.project.fastestdeliverypath.entity.Road;
import com.project.fastestdeliverypath.service.RoadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for managing roads in the delivery network.
 */
@RestController
@RequestMapping("/roads")
@RequiredArgsConstructor
@Slf4j
public class RoadController {

    private final RoadService roadService;

    /**
     * Creates or updates roads in the network.
     * Accepts either a single road or a list of roads.
     * If cities don't exist, they are created automatically.
     * 
     * @param roadRequests list of road requests to create/update
     * @return ResponseEntity with created roads and HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<List<RoadDTO>> createOrUpdateRoads(@Valid @RequestBody List<RoadRequest> roadRequests) {
        log.info("Received request to create/update {} road(s)", roadRequests.size());
        
        List<Road> roads = roadService.createOrUpdateRoads(roadRequests);
        
        List<RoadDTO> roadDTOs = roads.stream()
                .map(road -> new RoadDTO(
                        road.getFromCity().getName(),
                        road.getToCity().getName(),
                        road.getTravelTimeMinutes()
                ))
                .collect(Collectors.toList());
        
        log.info("Successfully created/updated {} road(s)", roads.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(roadDTOs);
    }
}

