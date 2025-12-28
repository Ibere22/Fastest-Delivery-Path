package com.project.fastestdeliverypath.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.fastestdeliverypath.dto.RouteRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RouteController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoadRepository roadRepository;

    @Autowired
    private CityRepository cityRepository;

    private City tbilisi;
    private City batumi;
    private City kutaisi;
    private City gonio;

    @BeforeEach
    void setUp() {
        roadRepository.deleteAll();
        cityRepository.deleteAll();

        tbilisi = cityRepository.save(new City("TBILISI"));
        batumi = cityRepository.save(new City("BATUMI"));
        kutaisi = cityRepository.save(new City("KUTAISI"));
        gonio = cityRepository.save(new City("GONIO"));
    }

    /**
     * Test 1: Find simple direct path
     */
    @Test
    void testFindFastestRoute_DirectPath() throws Exception {
        roadRepository.save(new Road(tbilisi, batumi, 360));

        RouteRequest request = new RouteRequest("Tbilisi", "Batumi");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pathCities", hasSize(2)))
                .andExpect(jsonPath("$.pathCities[0]", is("TBILISI")))
                .andExpect(jsonPath("$.pathCities[1]", is("BATUMI")))
                .andExpect(jsonPath("$.totalTravelTimeMinutes", is(360)))
                .andExpect(jsonPath("$.pathRoads", hasSize(1)))
                .andExpect(jsonPath("$.pathRoads[0].fromCity", is("TBILISI")))
                .andExpect(jsonPath("$.pathRoads[0].toCity", is("BATUMI")))
                .andExpect(jsonPath("$.pathRoads[0].travelTimeMinutes", is(360)));
    }

    /**
     * Test 2: Find path with multiple cities
     */
    @Test
    void testFindFastestRoute_MultipleHops() throws Exception {
        roadRepository.save(new Road(tbilisi, batumi, 360));
        roadRepository.save(new Road(batumi, gonio, 45));

        RouteRequest request = new RouteRequest("Tbilisi", "Gonio");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pathCities", hasSize(3)))
                .andExpect(jsonPath("$.pathCities[0]", is("TBILISI")))
                .andExpect(jsonPath("$.pathCities[1]", is("BATUMI")))
                .andExpect(jsonPath("$.pathCities[2]", is("GONIO")))
                .andExpect(jsonPath("$.totalTravelTimeMinutes", is(405)))
                .andExpect(jsonPath("$.pathRoads", hasSize(2)));
    }

    /**
     * Test 3: Find best path from multiple options
     */
    @Test
    void testFindFastestRoute_ChooseShortestPath() throws Exception {
        // Arrange - Create two paths to Gonio

        roadRepository.save(new Road(tbilisi, batumi, 360));
        roadRepository.save(new Road(batumi, gonio, 45));

        roadRepository.save(new Road(tbilisi, kutaisi, 240));
        roadRepository.save(new Road(kutaisi, gonio, 300));

        RouteRequest request = new RouteRequest("Tbilisi", "Gonio");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTravelTimeMinutes", is(405)))
                .andExpect(jsonPath("$.pathCities[1]", is("BATUMI"))); // Confirms it went via Batumi
    }

    /**
     * Test 4: No route between cities
     */
    @Test
    void testFindFastestRoute_NoRouteExists() throws Exception {
        roadRepository.save(new Road(kutaisi, batumi, 100));

        RouteRequest request = new RouteRequest("Tbilisi", "Gonio");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("No route found")));
    }

    /**
     * Test 5: No source city
     */
    @Test
    void testFindFastestRoute_SourceCityNotFound() throws Exception {
        RouteRequest request = new RouteRequest("NonExistent", "Batumi");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Source city not found")));
    }

    /**
     * Test 6: No destination city
     */
    @Test
    void testFindFastestRoute_DestinationCityNotFound() throws Exception {
        RouteRequest request = new RouteRequest("Tbilisi", "NonExistent");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Destination city not found")));
    }

    /**
     * Test 7: Check Case-insensitive city names
     */
    @Test
    void testFindFastestRoute_CaseInsensitive() throws Exception {
        roadRepository.save(new Road(tbilisi, batumi, 360));

        RouteRequest request = new RouteRequest("tbilisi", "batumi");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTravelTimeMinutes", is(360)));
    }

    /**
     * Test 8: Validation error - empty source city
     */
    @Test
    void testFindFastestRoute_EmptySourceCity_BadRequest() throws Exception {
        // Arrange
        RouteRequest request = new RouteRequest("", "Batumi");

        // Act & Assert
        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test 9: Complex network with cycles
     */
    @Test
    void testFindFastestRoute_WithCycles() throws Exception {
        roadRepository.save(new Road(tbilisi, kutaisi, 100));
        roadRepository.save(new Road(kutaisi, batumi, 100));
        roadRepository.save(new Road(batumi, tbilisi, 100)); // Cycle back to Tbilisi
        roadRepository.save(new Road(tbilisi, gonio, 500));   // Direct but longer path

        RouteRequest request = new RouteRequest("Tbilisi", "Gonio");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTravelTimeMinutes", is(500)))
                .andExpect(jsonPath("$.pathCities", hasSize(2))); // Direct path
    }

    /**
     * Test 10: Same source and destination
     */
    @Test
    void testFindFastestRoute_SameSourceAndDestination() throws Exception {
        RouteRequest request = new RouteRequest("Tbilisi", "Tbilisi");

        mockMvc.perform(post("/routes/fastest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pathCities", hasSize(1)))
                .andExpect(jsonPath("$.pathCities[0]", is("TBILISI")))
                .andExpect(jsonPath("$.totalTravelTimeMinutes", is(0)));
    }
}
