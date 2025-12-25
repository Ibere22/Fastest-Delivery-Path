package com.project.fastestdeliverypath.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "fromCity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Road> outgoingRoads = new ArrayList<>();

    @OneToMany(mappedBy = "toCity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Road> incomingRoads = new ArrayList<>();

    public City(String name) {
        this.name = name;
    }
}

