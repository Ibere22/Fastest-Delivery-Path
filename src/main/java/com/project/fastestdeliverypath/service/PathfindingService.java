package com.project.fastestdeliverypath.service;

import com.project.fastestdeliverypath.dto.RoadDTO;
import com.project.fastestdeliverypath.dto.RouteResponse;
import com.project.fastestdeliverypath.entity.City;
import com.project.fastestdeliverypath.entity.Road;
import com.project.fastestdeliverypath.exception.NoRouteFoundException;
import com.project.fastestdeliverypath.repository.CityRepository;
import com.project.fastestdeliverypath.repository.RoadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathfindingService {

    private final CityRepository cityRepository;
    private final RoadRepository roadRepository;

    /**
     * Finds the fastest delivery path between two cities using Dijkstra's algorithm.
     *
     * @param sourceCity      the starting city name
     * @param destinationCity the destination city name
     * @return RouteResponse containing the path, roads used, and total time
     * @throws NoRouteFoundException if no path exists between the cities
     */
    @Transactional(readOnly = true)
    public RouteResponse findFastestPath(String sourceCity, String destinationCity) {
        // Normalize city names to uppercase
        String normalizedSource = sourceCity.trim().toUpperCase();
        String normalizedDestination = destinationCity.trim().toUpperCase();

        log.info("Finding fastest path from {} to {}", normalizedSource, normalizedDestination);

        // Check if both cities exist
        City source = cityRepository.findByName(normalizedSource)
                .orElseThrow(() -> new NoRouteFoundException("Source city not found: " + sourceCity));
        City destination = cityRepository.findByName(normalizedDestination)
                .orElseThrow(() -> new NoRouteFoundException("Destination city not found: " + destinationCity));

        // Load all roads from the database to build the graph
        List<Road> allRoads = roadRepository.findAll();
        
        // Build an adjacency, Map: City Name -> List of (Neighbor City Name, Travel Time, Road)
        Map<String, List<Edge>> graph = buildGraph(allRoads);

        DijkstraResult result = runDijkstra(graph, normalizedSource, normalizedDestination);

        // Check if destination is reachable
        Integer destinationDistance = result.distances.get(normalizedDestination);
        if (destinationDistance == null || destinationDistance == Integer.MAX_VALUE) {
            throw new NoRouteFoundException("No route found between " + sourceCity + " and " + destinationCity + ".");
        }

        // Reconstruct the path
        List<String> pathCities = reconstructPath(result.parent, normalizedSource, normalizedDestination);
        
        // Create the list of roads used
        List<RoadDTO> pathRoads = buildPathRoads(pathCities, graph);
        
        // Get total travel time
        int totalTime = result.distances.get(normalizedDestination);

        log.info("Found path with {} cities and total time {} minutes", pathCities.size(), totalTime);

        return new RouteResponse(pathCities, pathRoads, totalTime);
    }

    /**
     * Builds an adjacency list representation of the road network.
     * Each city maps to a list of edges (outgoing roads).
     */
    private Map<String, List<Edge>> buildGraph(List<Road> roads) {
        Map<String, List<Edge>> graph = new HashMap<>();

        for (Road road : roads) {
            String fromCity = road.getFromCity().getName();
            String toCity = road.getToCity().getName();
            int travelTime = road.getTravelTimeMinutes();

            // Add edge from fromCity to toCity
            graph.computeIfAbsent(fromCity, k -> new ArrayList<>())
                    .add(new Edge(toCity, travelTime, road));
        }

        return graph;
    }

    /**
     * Runs Dijkstra's algorithm to find shortest paths from source to all reachable cities.
     *
     * @param graph       the adjacency list
     * @param source      the starting city
     * @param destination the target city
     * @return DijkstraResult containing distances and parent map
     */
    private DijkstraResult runDijkstra(Map<String, List<Edge>> graph, String source, String destination) {
        Map<String, Integer> distances = new HashMap<>();
        
        // Needed for reconstructing the path
        Map<String, String> parent = new HashMap<>();
        
        // Priority queue orders cities by their current shortest distance
        PriorityQueue<CityDistance> pq = new PriorityQueue<>(Comparator.comparingInt(cd -> cd.distance));

        distances.put(source, 0);
        pq.offer(new CityDistance(source, 0));

        while (!pq.isEmpty()) {
            CityDistance current = pq.poll();
            String currentCity = current.cityName;
            int currentDistance = current.distance;

            // If we reach destination, we can stop
            if (currentCity.equals(destination)) {
                break;
            }

            // Skip if we already found a better path
            if (currentDistance > distances.getOrDefault(currentCity, Integer.MAX_VALUE)) {
                continue;
            }

            // Check all neighbors
            List<Edge> edges = graph.getOrDefault(currentCity, Collections.emptyList());
            for (Edge edge : edges) {
                String neighbor = edge.toCity;
                int newDistance = currentDistance + edge.travelTime;

                // If we found a shorter path to neighbor, update it
                int oldDistance = distances.getOrDefault(neighbor, Integer.MAX_VALUE);
                if (newDistance < oldDistance) {
                    distances.put(neighbor, newDistance);
                    parent.put(neighbor, currentCity);
                    pq.offer(new CityDistance(neighbor, newDistance));
                }
            }
        }

        return new DijkstraResult(distances, parent);
    }


    private List<String> reconstructPath(Map<String, String> parent, String source, String destination) {
        List<String> path = new ArrayList<>();
        String current = destination;

        // Backtrack from destination to source using parent map
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }

        // Reverse the path because it waas built backwards)
        Collections.reverse(path);
        
        return path;
    }


    private List<RoadDTO> buildPathRoads(List<String> pathCities, Map<String, List<Edge>> graph) {
        List<RoadDTO> pathRoads = new ArrayList<>();

        for (int i = 0; i < pathCities.size() - 1; i++) {
            String fromCity = pathCities.get(i);
            String toCity = pathCities.get(i + 1);

            List<Edge> edges = graph.get(fromCity);
            for (Edge edge : edges) {
                if (edge.toCity.equals(toCity)) {
                    pathRoads.add(new RoadDTO(fromCity, toCity, edge.travelTime));
                    break;
                }
            }
        }

        return pathRoads;
    }

    // Helper classes for Dijkstra's algorithm


    private static class Edge {
        String toCity;
        int travelTime;
        Road road;

        Edge(String toCity, int travelTime, Road road) {
            this.toCity = toCity;
            this.travelTime = travelTime;
            this.road = road;
        }
    }


    private static class CityDistance {
        String cityName;
        int distance;

        CityDistance(String cityName, int distance) {
            this.cityName = cityName;
            this.distance = distance;
        }
    }


    private static class DijkstraResult {
        Map<String, Integer> distances;
        Map<String, String> parent;

        DijkstraResult(Map<String, Integer> distances, Map<String, String> parent) {
            this.distances = distances;
            this.parent = parent;
        }
    }
}

