package com.project.fastestdeliverypath.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.fastestdeliverypath.dto.RoadRequest;
import com.project.fastestdeliverypath.entity.City;
import com.project.fastestdeliverypath.entity.Road;
import com.project.fastestdeliverypath.repository.CityRepository;
import com.project.fastestdeliverypath.repository.RoadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RoadController.
 * Tests the full flow: HTTP request → Controller → Service → Repository → Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoadRepository roadRepository;

    @Autowired
    private CityRepository cityRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        roadRepository.deleteAll();
        cityRepository.deleteAll();
    }

    /**
     * Test 1: Create a single road successfully
     */
    @Test
    void testCreateRoad_Success() throws Exception {
        RoadRequest request = new RoadRequest("Tbilisi", "Batumi", 360);
        List<RoadRequest> requests = Arrays.asList(request);

        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].travelTimeMinutes", is(360)));

        // Check in db
        List<City> cities = cityRepository.findAll();
        assertEquals(2, cities.size());

        List<Road> roads = roadRepository.findAll();
        assertEquals(1, roads.size());
        assertEquals(360, roads.get(0).getTravelTimeMinutes());
    }

    /**
     * Test 2: Create multiple roads at once
     */
    @Test
    void testCreateMultipleRoads_Success() throws Exception {
        List<RoadRequest> requests = Arrays.asList(
                new RoadRequest("Tbilisi", "Batumi", 360),
                new RoadRequest("Batumi", "Gonio", 45),
                new RoadRequest("Tbilisi", "Kutaisi", 240)
        );

        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)));

        List<City> cities = cityRepository.findAll();
        assertEquals(4, cities.size()); // Tbilisi, Batumi, Gonio, Kutaisi

        List<Road> roads = roadRepository.findAll();
        assertEquals(3, roads.size());
    }

    /**
     * Test 3: Cities are auto-created if they don't exist
     */
    @Test
    void testCreateRoad_AutoCreateCities() throws Exception {
        RoadRequest request = new RoadRequest("Paris", "London", 120);
        List<RoadRequest> requests = Arrays.asList(request);

        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated());

        assertTrue(cityRepository.findByName("PARIS").isPresent());
        assertTrue(cityRepository.findByName("LONDON").isPresent());
    }

    /**
     * Test 4: Update already created road
     */
    @Test
    void testUpdateRoad_Success() throws Exception {
        City tbilisi = cityRepository.save(new City("TBILISI"));
        City batumi = cityRepository.save(new City("BATUMI"));
        roadRepository.save(new Road(tbilisi, batumi, 360));

        RoadRequest updateRequest = new RoadRequest("Tbilisi", "Batumi", 400);
        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(updateRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].travelTimeMinutes", is(400)));

        List<Road> roads = roadRepository.findAll();
        assertEquals(1, roads.size());
        assertEquals(400, roads.get(0).getTravelTimeMinutes());
    }

    /**
     * Test 5: Negative travel time error
     */
    @Test
    void testCreateRoad_NegativeTravelTime_BadRequest() throws Exception {
        RoadRequest request = new RoadRequest("Tbilisi", "Batumi", -100);
        List<RoadRequest> requests = Arrays.asList(request);

        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());

        assertEquals(0, roadRepository.count());
        assertEquals(0, cityRepository.count());
    }

    /**
     * Test 6: Empty city name error
     */
    @Test
    void testCreateRoad_EmptyCityName_BadRequest() throws Exception {
        RoadRequest request = new RoadRequest("", "Batumi", 360);
        List<RoadRequest> requests = Arrays.asList(request);

        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test 7: Check Case-insensitive city names
     */
    @Test
    void testCreateRoad_CaseInsensitive() throws Exception {
        RoadRequest request1 = new RoadRequest("tbilisi", "batumi", 360);
        mockMvc.perform(post("/roads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList(request1))));

        RoadRequest request2 = new RoadRequest("TBILISI", "kutaisi", 240);
        mockMvc.perform(post("/roads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(request2))))
                .andExpect(status().isCreated());

        // Assert - Should have 3 cities (not 4)
        List<City> cities = cityRepository.findAll();
        assertEquals(3, cities.size()); // TBILISI, BATUMI, KUTAISI

        List<Road> roads = roadRepository.findAll();
        assertEquals(2, roads.size());
    }
}
