package com.project.fastestdeliverypath.repository;

import com.project.fastestdeliverypath.entity.City;
import com.project.fastestdeliverypath.entity.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadRepository extends JpaRepository<Road, Long> {
    
    List<Road> findByFromCity(City fromCity);
    
    Optional<Road> findByFromCityAndToCity(City fromCity, City toCity);
}

