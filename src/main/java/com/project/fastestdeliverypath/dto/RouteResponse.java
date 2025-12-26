package com.project.fastestdeliverypath.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private List<String> pathCities;
    private List<RoadDTO> pathRoads;
    private Integer totalTravelTimeMinutes;
}

