package com.season.semiproject.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.vo.LocationRequest;

@RestController
@RequestMapping("/api")
public class LocationController {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private double currentAltitude = 0.0;
    private float currentSpeed = 0;
    private float currentBearing = 0;
    private String currentTime = "";

    @PostMapping("/recordlocation")
    public ResponseEntity<String> updateLocation(@RequestBody LocationRequest locationRequest) {
        currentLatitude = locationRequest.getLatitude();
        currentLongitude = locationRequest.getLongitude();
        currentAltitude = locationRequest.getAltitude();
        currentSpeed = locationRequest.getSpeed();
        currentBearing = locationRequest.getBearing();
        currentTime = locationRequest.getTime();
        
        return ResponseEntity.ok("Location updated successfully");
    }

    @GetMapping("/recordlocation")
    public Map<String, Object> getLocation() {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", currentLatitude);
        locationData.put("longitude", currentLongitude);
        locationData.put("altitude", currentAltitude);
        locationData.put("speed", currentSpeed);
        locationData.put("bearing", currentBearing);
        locationData.put("time", currentTime);
        return locationData;
    }
}

