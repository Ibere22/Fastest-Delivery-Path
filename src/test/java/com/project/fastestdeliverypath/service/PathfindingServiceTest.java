package com.project.fastestdeliverypath.service;

import com.project.fastestdeliverypath.dto.RouteResponse;
import com.project.fastestdeliverypath.entity.City;
import com.project.fastestdeliverypath.entity.Road;
import com.project.fastestdeliverypath.exception.NoRouteFoundException;
import com.project.fastestdeliverypath.repository.CityRepository;
import com.project.fastestdeliverypath.repository.RoadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
    * Unit tests for PathfindingService
 */
@ExtendWith(MockitoExtension.class)
class PathfindingServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RoadRepository roadRepository;

    @InjectMocks
    private PathfindingService pathfindingService;

    private City tbilisi;
    private City batumi;
    private City kutaisi;
    private City gonio;

    @BeforeEach
    void setUp() {
        tbilisi = new City(1L, "TBILISI", new java.util.ArrayList<>(), new java.util.ArrayList<>());
        batumi = new City(2L, "BATUMI", new java.util.ArrayList<>(), new java.util.ArrayList<>());
        kutaisi = new City(3L, "KUTAISI", new java.util.ArrayList<>(), new java.util.ArrayList<>());
        gonio = new City(4L, "GONIO", new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    /**
     * Test 1: Simple path
     */
    @Test
    void testFindFastestPath_SimplePath() {
        City source = tbilisi;
        City destination = batumi;

        Road road1 = new Road(1L, source, destination, 360);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(source));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(destination));
        when(roadRepository.findAll()).thenReturn(Arrays.asList(road1));

        RouteResponse result = pathfindingService.findFastestPath("Tbilisi", "Batumi");

        assertEquals(2, result.getPathCities().size());
        assertEquals("TBILISI", result.getPathCities().get(0));
        assertEquals("BATUMI", result.getPathCities().get(1));
        assertEquals(360, result.getTotalTravelTimeMinutes());
        assertEquals(1, result.getPathRoads().size());
    }

    /**
     * Test 2: Path with multiple cities
     */
    @Test
    void testFindFastestPath_MultipleCities() {
        City source = tbilisi;
        City mid = kutaisi;
        City destination = gonio;

        // Direct path: Tbilisi -> Batumi -> Gonio (405 minutes)
        Road road1 = new Road(1L, source, batumi, 360);
        Road road2 = new Road(2L, batumi, destination, 45);

        // Shorter path: Tbilisi -> Kutaisi -> Gonio (540 minutes)
        Road road3 = new Road(3L, source, mid, 240);
        Road road4 = new Road(4L, mid, destination, 300);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(source));
        when(cityRepository.findByName("GONIO")).thenReturn(Optional.of(destination));
        when(roadRepository.findAll()).thenReturn(Arrays.asList(road1, road2, road3, road4));

        RouteResponse result = pathfindingService.findFastestPath("Tbilisi", "Gonio");

        // Should choose path: Tbilisi -> Batumi -> Gonio (405 minutes)
        assertEquals(3, result.getPathCities().size());
        assertEquals("TBILISI", result.getPathCities().get(0));
        assertEquals("BATUMI", result.getPathCities().get(1));
        assertEquals("GONIO", result.getPathCities().get(2));
        assertEquals(405, result.getTotalTravelTimeMinutes());
    }

    /**
     * Test 3: Case-insensitive city names
     */
    @Test
    void testFindFastestPath_CaseInsensitive() {
        City source = tbilisi;
        City destination = batumi;
        Road road1 = new Road(1L, source, destination, 360);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(source));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(destination));
        when(roadRepository.findAll()).thenReturn(Arrays.asList(road1));

        RouteResponse result = pathfindingService.findFastestPath("tbilisi", "batumi");

        assertNotNull(result);
        assertEquals(360, result.getTotalTravelTimeMinutes());
    }

    /**
     * Test 4: No source city
     */
    @Test
    void testFindFastestPath_SourceCityNotFound() {
        when(cityRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(NoRouteFoundException.class, () -> {
            pathfindingService.findFastestPath("NonExistent", "Batumi");
        });
    }

    /**
     * Test 5: No destination city
     */
    @Test
    void testFindFastestPath_DestinationCityNotFound() {
        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(tbilisi));
        when(cityRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(NoRouteFoundException.class, () -> {
            pathfindingService.findFastestPath("Tbilisi", "NonExistent");
        });
    }

    /**
     * Test 6: No path between cities
     */
    @Test
    void testFindFastestPath_NoRouteExists() {
        City source = tbilisi;
        City destination = batumi;
        City source2 = kutaisi;
        City dest2 = gonio;

        Road road1 = new Road(1L, source2, dest2, 100);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(source));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(destination));
        when(roadRepository.findAll()).thenReturn(Arrays.asList(road1));

        assertThrows(NoRouteFoundException.class, () -> {
            pathfindingService.findFastestPath("Tbilisi", "Batumi");
        });
    }

    /**
     * Test 7: Graph with cycles
     */
    @Test
    void testFindFastestPath_WithCycles() {
        City source = tbilisi;
        City destination = gonio;

        // Create cycle: Tbilisi -> Kutaisi -> Batumi -> Tbilisi
        Road road1 = new Road(1L, source, kutaisi, 100);
        Road road2 = new Road(2L, kutaisi, batumi, 100);
        Road road3 = new Road(3L, batumi, source, 100);
        Road road4 = new Road(4L, source, gonio, 500);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(source));
        when(cityRepository.findByName("GONIO")).thenReturn(Optional.of(destination));
        when(roadRepository.findAll()).thenReturn(Arrays.asList(road1, road2, road3, road4));

        RouteResponse result = pathfindingService.findFastestPath("Tbilisi", "Gonio");

        assertEquals(2, result.getPathCities().size());
        assertEquals(500, result.getTotalTravelTimeMinutes());
    }

    /**
     * Test 8: Same source and destination
     */
    @Test
    void testFindFastestPath_SameSourceAndDestination() {
        City source = tbilisi;

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(source));
        when(roadRepository.findAll()).thenReturn(Arrays.asList());

        RouteResponse result = pathfindingService.findFastestPath("Tbilisi", "Tbilisi");

        assertEquals(1, result.getPathCities().size());
        assertEquals("TBILISI", result.getPathCities().get(0));
        assertEquals(0, result.getTotalTravelTimeMinutes());
    }
}

