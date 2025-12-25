package com.project.fastestdeliverypath.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roads", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"from_city_id", "to_city_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Road {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_city_id", nullable = false)
    private City fromCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_city_id", nullable = false)
    private City toCity;

    @Column(nullable = false)
    @Min(value = 0, message = "Travel time must be non-negative")
    private Integer travelTimeMinutes;

    public Road(City fromCity, City toCity, Integer travelTimeMinutes) {
        this.fromCity = fromCity;
        this.toCity = toCity;
        this.travelTimeMinutes = travelTimeMinutes;
    }
}

