package com.season.semiproject.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.handler.NotificationWebSocketHandler;
import com.season.semiproject.service.NotificationService;
import com.season.semiproject.vo.NotificationRequest;



@RestController
@RequestMapping("/api")
public class HelpCall_LocationController {

    private static final Logger logger = LoggerFactory.getLogger(HelpCall_LocationController.class);

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    private final NotificationService notificationService;

    private final NotificationWebSocketHandler webSocketHandler;

    @Autowired
    public HelpCall_LocationController(NotificationService notificationService) {
        this.notificationService = notificationService;
      this.webSocketHandler = new NotificationWebSocketHandler();
    }

 // userId별 위치 데이터를 저장할 Map
    private final Map<String, Map<String, Double>> userLocations = new HashMap<>();

    @PostMapping("/location")
    public ResponseEntity<String> updateLocation(@RequestBody NotificationRequest notificationRequest) {
        String userId = notificationRequest.getUserId();
        double latitude = notificationRequest.getLatitude();
        double longitude = notificationRequest.getLongitude();

        if (userId == null || userId.isEmpty()) {
            logger.warn("userId가 전달되지 않았습니다.");
            return ResponseEntity.badRequest().body("Invalid userId");
        }

        // userId별 위치 데이터 저장
        userLocations.put(userId, Map.of("latitude", latitude, "longitude", longitude));
        // WebSocket으로 업데이트된 위치 전송
        try {
            webSocketHandler.sendLocationUpdate(userId, latitude, longitude);
        } catch (IOException e) {
            logger.error("WebSocket 전송 오류", e);
        }

        return ResponseEntity.ok("Location updated successfully");
    }

    @GetMapping("/location")
    public ResponseEntity<Map<String, Map<String, Double>>> getAllUserLocations() {
        // 모든 사용자 위치 반환
        return ResponseEntity.ok(userLocations);
    }
}