package com.project.fastestdeliverypath.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadDTO {
    private String fromCity;
    private String toCity;
    private Integer travelTimeMinutes;
}

