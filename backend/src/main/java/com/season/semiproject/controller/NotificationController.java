package com.season.semiproject.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.dao.UserDAO;
import com.season.semiproject.service.NotificationService;
import com.season.semiproject.vo.LocationRequest;
import com.season.semiproject.vo.NotificationRequest;


@RequestMapping("/api")
@RestController
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;
    private final UserDAO userDAO;

    @Autowired
    public NotificationController(NotificationService notificationService, UserDAO userDAO) {
        this.notificationService = notificationService;
        this.userDAO = userDAO;
    }

    @PostMapping("/sendNotification")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest notificationRequest) {
        String userId = notificationRequest.getUserId();
        String userNickName = userDAO.getUserNickNameByUserId(userId);

        // sendNotification의 title과 body 설정
        String title = "긴급 SOS 알림";
        String body = "긴급 SOS 요청이 보내졌습니다.";

        logger.info("SOS 알림 전송 요청 수신: 사용자 ID - {}", userNickName);

        if (userId == null || userId.isEmpty()) {
            logger.warn("사용자 ID가 유효하지 않습니다.");
            return ResponseEntity.badRequest().body("사용자 ID가 유효하지 않습니다.");
        }

        try {
            notificationService.sendNotificationToUser(userId, title, body);  // 특정 사용자에게 개별 SOS 알림 전송
            return ResponseEntity.ok("SOS 알림 전송 성공");
        } catch (Exception e) {
            logger.error("SOS 알림 전송 중 오류 발생: ", e);
            return ResponseEntity.status(500).body("SOS 알림 전송 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/sendNotificationToAll")
    public ResponseEntity<String> sendNotificationToAll(@RequestBody NotificationRequest notificationRequest) {
        String userId = notificationRequest.getUserId();
        String title = "긴급 SOS 알림";
        String body = "사용자 " + userId + "님이 SOS 요청을 보냈습니다!";
        
        double latitude = notificationRequest.getLatitude();
        double longitude = notificationRequest.getLongitude();

        try {
            notificationService.sendNotificationToAllUsers(title, body, latitude, longitude);
            return ResponseEntity.ok("모든 사용자에게 알림 전송 성공");
        } catch (Exception e) {
            logger.error("모든 사용자에게 알림 전송 중 오류 발생: ", e);
            return ResponseEntity.status(500).body("모든 사용자에게 알림 전송 중 오류가 발생했습니다.");
        }
    }


    @PostMapping("/saveNotificationData")
    public ResponseEntity<String> saveNotificationData(@RequestBody NotificationRequest notificationRequest) {
        logger.info("FCM 토큰 저장 요청 수신: {}", notificationRequest);

        String userId = notificationRequest.getUserId();
        String token = notificationRequest.getToken();

        if (userId == null || userId.isEmpty()) {
            logger.warn("유효하지 않은 사용자 ID입니다.");
            return ResponseEntity.badRequest().body("유효하지 않은 사용자 ID입니다.");
        }

        try {
            // FCM 토큰이 없는 경우 기존 토큰 가져오기
            if (token == null || token.isEmpty()) {
                token = notificationService.getFCMTokenByUserId(userId);
                if (token == null || token.isEmpty()) {
                    logger.warn("저장된 FCM 토큰이 없습니다. 요청을 무시합니다.");
                    return ResponseEntity.badRequest().body("FCM 토큰이 null이거나 비어있습니다.");
                }
                logger.info("기존에 저장된 FCM 토큰을 사용합니다: {}", token);
            }

            notificationRequest.setToken(token);
            notificationService.saveOrUpdateNotificationData(notificationRequest);
            return ResponseEntity.ok("FCM 토큰과 알림 허용 상태가 저장되었습니다.");
        } catch (Exception e) {
            logger.error("알림 데이터 저장 중 오류 발생: ", e);
            return ResponseEntity.status(500).body("알림 데이터 저장 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/getFCMToken")
    public ResponseEntity<String> getFCMToken(@RequestParam String userId) {
        logger.info("FCM 토큰 요청: userId = {}", userId);

        if (userId == null || userId.isEmpty()) {
            logger.warn("유효하지 않은 사용자 ID입니다.");
            return ResponseEntity.badRequest().body("유효하지 않은 사용자 ID입니다.");
        }

        try {
            String fcmToken = notificationService.getFCMTokenByUserId(userId);
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.warn("FCM 토큰을 찾을 수 없습니다: userId = {}", userId);
                return ResponseEntity.status(404).body("FCM 토큰을 찾을 수 없습니다.");
            }

            logger.info("FCM 토큰 반환: userId = {}", userId);
            return ResponseEntity.ok(fcmToken);
        } catch (Exception e) {
            logger.error("FCM 토큰 조회 중 오류 발생: ", e);
            return ResponseEntity.status(500).body("FCM 토큰 조회 중 오류가 발생했습니다.");
        }
    } 
}