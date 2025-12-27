package com.project.fastestdeliverypath.service;

import com.project.fastestdeliverypath.dto.RoadRequest;
import com.project.fastestdeliverypath.entity.City;
import com.project.fastestdeliverypath.entity.Road;
import com.project.fastestdeliverypath.exception.InvalidRoadException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
    * Unit tests for RoadService
 */
@ExtendWith(MockitoExtension.class)
class RoadServiceTest {

    @Mock
    private RoadRepository roadRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private RoadService roadService;

    private City tbilisi;
    private City batumi;

    @BeforeEach
    void setUp() {
        tbilisi = new City(1L, "TBILISI", new java.util.ArrayList<>(), new java.util.ArrayList<>());
        batumi = new City(2L, "BATUMI", new java.util.ArrayList<>(), new java.util.ArrayList<>());
    }

    /**
     * Test 1: Create a single road
     */
    @Test
    void testCreateOrUpdateRoads_BothCitiesExist() {
        RoadRequest request = new RoadRequest("Tbilisi", "Batumi", 360);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(tbilisi));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(batumi));
        when(roadRepository.findByFromCityAndToCity(tbilisi, batumi)).thenReturn(Optional.empty());

        Road savedRoad = new Road(1L, tbilisi, batumi, 360);
        when(roadRepository.save(any(Road.class))).thenReturn(savedRoad);

        List<Road> result = roadService.createOrUpdateRoads(Arrays.asList(request));

        assertEquals(1, result.size());
        assertEquals(360, result.get(0).getTravelTimeMinutes());
        verify(cityRepository, times(2)).findByName(anyString());
        verify(roadRepository).save(any(Road.class));
    }

    /**
     * Test 2: Create cities if they don't exist
     */
    @Test
    void testCreateOrUpdateRoads_AutoCreateCities() {
        RoadRequest request = new RoadRequest("Paris", "London", 120);

        City paris = new City(3L, "PARIS", new java.util.ArrayList<>(), new java.util.ArrayList<>());
        City london = new City(4L, "LONDON", new java.util.ArrayList<>(), new java.util.ArrayList<>());

        when(cityRepository.findByName("PARIS")).thenReturn(Optional.empty());
        when(cityRepository.findByName("LONDON")).thenReturn(Optional.empty());
        when(cityRepository.save(any(City.class)))
                .thenReturn(paris)
                .thenReturn(london);
        when(roadRepository.findByFromCityAndToCity(paris, london)).thenReturn(Optional.empty());

        Road savedRoad = new Road(2L, paris, london, 120);
        when(roadRepository.save(any(Road.class))).thenReturn(savedRoad);

        List<Road> result = roadService.createOrUpdateRoads(Arrays.asList(request));

        assertEquals(1, result.size());
        verify(cityRepository, times(2)).save(any(City.class));
        verify(roadRepository).save(any(Road.class));
    }

    /**
     * Test 3: Update already created road
     */
    @Test
    void testCreateOrUpdateRoads_UpdateExistingRoad() {
        RoadRequest request = new RoadRequest("Tbilisi", "Batumi", 400);

        Road existingRoad = new Road(1L, tbilisi, batumi, 360);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(tbilisi));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(batumi));
        when(roadRepository.findByFromCityAndToCity(tbilisi, batumi)).thenReturn(Optional.of(existingRoad));

        Road updatedRoad = new Road(1L, tbilisi, batumi, 400);
        when(roadRepository.save(any(Road.class))).thenReturn(updatedRoad);

        List<Road> result = roadService.createOrUpdateRoads(Arrays.asList(request));

        assertEquals(1, result.size());
        assertEquals(400, result.get(0).getTravelTimeMinutes());
    }

    /**
     * Test 4: Reject self-loop
     */
    @Test
    void testCreateOrUpdateRoads_SelfLoop_ThrowsException() {
        RoadRequest request = new RoadRequest("Tbilisi", "Tbilisi", 100);

        assertThrows(InvalidRoadException.class, () -> {
            roadService.createOrUpdateRoads(Arrays.asList(request));
        });
    }

    /**
     * Test 5: Reject negative travel time error
     */
    @Test
    void testCreateOrUpdateRoads_NegativeTravelTime_ThrowsException() {
        RoadRequest request = new RoadRequest("Tbilisi", "Batumi", -100);

        assertThrows(InvalidRoadException.class, () -> {
            roadService.createOrUpdateRoads(Arrays.asList(request));
        });
    }

    /**
     * Test 6: Reject empty city names error
     */
    @Test
    void testCreateOrUpdateRoads_EmptyFromCity_ThrowsException() {  
        RoadRequest request = new RoadRequest("", "Batumi", 360);

        assertThrows(InvalidRoadException.class, () -> {
            roadService.createOrUpdateRoads(Arrays.asList(request));
        });
    }

    /**
     * Test 7: Check Case-insensitive city names
     */
    @Test
    void testCreateOrUpdateRoads_CaseInsensitiveNames() {
        RoadRequest request = new RoadRequest("tbilisi", "batumi", 360);

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(tbilisi));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(batumi));
        when(roadRepository.findByFromCityAndToCity(tbilisi, batumi)).thenReturn(Optional.empty());

        Road savedRoad = new Road(1L, tbilisi, batumi, 360);
        when(roadRepository.save(any(Road.class))).thenReturn(savedRoad);

        List<Road> result = roadService.createOrUpdateRoads(Arrays.asList(request));

        assertEquals(1, result.size());
        verify(cityRepository).findByName("TBILISI");
        verify(cityRepository).findByName("BATUMI");
    }

    /**
     * Test 8: Multiple roads at once
     */
    @Test
    void testCreateOrUpdateRoads_MultipleRoads() {
        List<RoadRequest> requests = Arrays.asList(
                new RoadRequest("Tbilisi", "Batumi", 360),
                new RoadRequest("Batumi", "Gonio", 45)
        );

        City gonio = new City(5L, "GONIO", new java.util.ArrayList<>(), new java.util.ArrayList<>());

        when(cityRepository.findByName("TBILISI")).thenReturn(Optional.of(tbilisi));
        when(cityRepository.findByName("BATUMI")).thenReturn(Optional.of(batumi));
        when(cityRepository.findByName("GONIO")).thenReturn(Optional.of(gonio));
        when(roadRepository.findByFromCityAndToCity(any(), any())).thenReturn(Optional.empty());

        Road road1 = new Road(1L, tbilisi, batumi, 360);
        Road road2 = new Road(2L, batumi, gonio, 45);
        when(roadRepository.save(any(Road.class)))
                .thenReturn(road1)
                .thenReturn(road2);

        List<Road> result = roadService.createOrUpdateRoads(requests);

        assertEquals(2, result.size());
        verify(roadRepository, times(2)).save(any(Road.class));
    }
}

