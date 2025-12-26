package com.project.fastestdeliverypath.service;

import com.project.fastestdeliverypath.dto.RoadRequest;
import com.project.fastestdeliverypath.entity.City;
import com.project.fastestdeliverypath.entity.Road;
import com.project.fastestdeliverypath.exception.InvalidRoadException;
import com.project.fastestdeliverypath.repository.CityRepository;
import com.project.fastestdeliverypath.repository.RoadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadService {

    private final RoadRepository roadRepository;
    private final CityRepository cityRepository;

    /**
     * Creates or updates roads based on the provided requests.
     * If cities don't exist, they are created automatically.
     *
     * @param roadRequests list of road requests
     * @return list of created/updated roads
     */
    @Transactional
    public List<Road> createOrUpdateRoads(List<RoadRequest> roadRequests) {
        List<Road> roads = new ArrayList<>();

        for (RoadRequest request : roadRequests) {
            validateRoadRequest(request);

            String fromCityName = request.getFromCity().trim().toUpperCase();
            String toCityName = request.getToCity().trim().toUpperCase();

            // Prevent self-loop
            if (fromCityName.equals(toCityName)) {
                throw new InvalidRoadException("A road cannot connect a city to itself: " + fromCityName);
            }

            City fromCity = getOrCreateCity(fromCityName);
            City toCity = getOrCreateCity(toCityName);

            // Check if road already exists
            Road road = roadRepository.findByFromCityAndToCity(fromCity, toCity)
                    .orElse(new Road());

            road.setFromCity(fromCity);
            road.setToCity(toCity);
            road.setTravelTimeMinutes(request.getTravelTimeMinutes());

            road = roadRepository.save(road);
            roads.add(road);

            log.info("Created/Updated road from {} to {} with travel time {} minutes",
                    fromCityName, toCityName, request.getTravelTimeMinutes());
        }

        return roads;
    }


    private City getOrCreateCity(String cityName) {
        return cityRepository.findByName(cityName)
                .orElseGet(() -> {
                    City newCity = new City(cityName);
                    City savedCity = cityRepository.save(newCity);
                    log.info("Created new city: {}", cityName);
                    return savedCity;
                });
    }


    private void validateRoadRequest(RoadRequest request) {
        if (request.getFromCity() == null || request.getFromCity().trim().isEmpty()) {
            throw new InvalidRoadException("From city cannot be empty");
        }
        if (request.getToCity() == null || request.getToCity().trim().isEmpty()) {
            throw new InvalidRoadException("To city cannot be empty");
        }
        if (request.getTravelTimeMinutes() == null || request.getTravelTimeMinutes() < 0) {
            throw new InvalidRoadException("Travel time must be non-negative");
        }
    }
}

