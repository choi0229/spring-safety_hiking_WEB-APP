package com.season.semiproject.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.season.semiproject.vo.LocationRequest;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    private final ConcurrentHashMap<String, double[]> userLocations = new ConcurrentHashMap<>();

    // 사용자 위치 업데이트
    public void updateUserLocation(String userId, double latitude, double longitude) {
        userLocations.put(userId, new double[]{latitude, longitude});
    }

    // 저장된 사용자 위치 반환
    public ConcurrentHashMap<String, double[]> getAllUserLocations() {
        return userLocations;
    }
}
