package com.project.fastestdeliverypath.controller;

import com.project.fastestdeliverypath.dto.RouteRequest;
import com.project.fastestdeliverypath.dto.RouteResponse;
import com.project.fastestdeliverypath.service.PathfindingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for finding delivery routes.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Slf4j
public class RouteController {

    private final PathfindingService pathfindingService;

    /**
     * Finds the fastest delivery path between two cities.
     * Uses Dijkstra's algorithm to calculate the shortest path based on travel time.
     *
     * @param routeRequest the source and destination cities
     * @return ResponseEntity with RouteResponse containing the fastest path
     */
    @PostMapping("/fastest")
    public ResponseEntity<RouteResponse> findFastestRoute(@Valid @RequestBody RouteRequest routeRequest) {
        log.info("Received request to find fastest route from {} to {}", 
                routeRequest.getSourceCity(), routeRequest.getDestinationCity());
        
        RouteResponse response = pathfindingService.findFastestPath(
                routeRequest.getSourceCity(), 
                routeRequest.getDestinationCity()
        );
        
        log.info("Successfully found route with total time: {} minutes", 
                response.getTotalTravelTimeMinutes());
        
        return ResponseEntity.ok(response);
    }
}

