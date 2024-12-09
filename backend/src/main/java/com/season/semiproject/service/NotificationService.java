package com.season.semiproject.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.season.semiproject.dao.NotificationDAO;
import com.season.semiproject.dao.UserDAO;
import com.season.semiproject.handler.NotificationWebSocketHandler;
import com.season.semiproject.vo.NotificationRequest;

@Service
public class NotificationService {

    private final UserDAO userDAO;
    private final NotificationDAO notificationDAO;
    private final NotificationWebSocketHandler webSocketHandler;
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 2000; // 재시도 간격(밀리초)

    
    
    private double currentLatitude;
    private double currentLongitude;

    public void updateLocation(double latitude, double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
    }

    @Autowired
    public NotificationService(UserDAO userDAO, NotificationDAO notificationDAO, NotificationWebSocketHandler webSocketHandler) {
        this.userDAO = userDAO;
        this.notificationDAO = notificationDAO;
        this.webSocketHandler = webSocketHandler;
    }
    
    // 모든 사용자에게 위도와 경도를 포함하여 알림을 보내는 메서드
    public void sendNotificationToAllUsers(String title, String body, double latitude, double longitude) {
        List<String> allTokens = notificationDAO.getAllFcmTokens();

        for (String token : allTokens) {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("latitude", String.valueOf(latitude))
                    .putData("longitude", String.valueOf(longitude))
                    .putData("click_action", "OPEN_HELPCALL")
                    .build();

            try {
                retrySendingNotification(message, MAX_RETRIES);
                logger.info("FCM 메시지 전송 성공: " + token);
            } catch (FirebaseMessagingException e) {
                logger.error("FCM 메시지 전송 실패: " + token, e);
            }
        }

        try {
            webSocketHandler.sendLocationUpdateToAll(latitude, longitude);
        } catch (IOException e) {
            logger.error("WebSocket SOS 알림 전송 실패", e);
        }
    }

    // 재시도 로직이 포함된 알림 전송 메서드
    private void retrySendingNotification(Message message, int retries) throws FirebaseMessagingException {
        for (int i = 0; i < retries; i++) {
            try {
                FirebaseMessaging.getInstance().send(message);
                return;
            } catch (FirebaseMessagingException e) {
                if (i == retries - 1) {
                    throw e;
                }
                logger.warn("알림 전송 실패, 재시도 중 (" + (i + 1) + "/" + retries + ")");
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void saveNotificationData(NotificationRequest notificationRequest) {
        notificationDAO.saveNotificationData(notificationRequest);
    }

    // 특정 사용자에게 알림을 보내는 메서드 (독립된 FCM 토큰 포함)
    public void sendNotificationToUser(String userId, String title, String body) {
        List<String> tokens = new ArrayList<>();

        // 사용자 ID로 FCM 토큰 조회
        String fcmToken = userDAO.getFcmTokenByUserId(userId);
        if (fcmToken != null) {
            tokens.add(fcmToken);  // 사용자 토큰 추가
        } else {
            logger.warn("사용자 ID: " + userId + "는 알림 허용이 거부되었거나 FCM 토큰이 없습니다.");
        }

        // 추가적인 독립된 FCM 토큰
        String independentToken = "ea6pENvmcDRfCcc2mABiXG:APA91bHkdUZm42YVJUCamOlIcz4Qc2YK6XYOD8YmMy74bHLrK2QEdfZsjkXDWpkZUGozzaCW47guXPyn5EsFKFsSKG0xij44FMfZz86Vj0nCegtI4_mUqL0";
        tokens.add(independentToken);

        for (String token : tokens) {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("click_action", "OPEN_HELPCALL")
                    .build();
            try {
                retrySendingNotification(message, MAX_RETRIES);
                logger.info("FCM 토큰으로 알림 전송 성공: " + token);
            } catch (FirebaseMessagingException e) {
                logger.error("FCM 토큰으로 알림 전송 실패: " + token + " - " + e.getMessage());
            }
        }
    }

 
    public String getFCMTokenByUserId(String userId) {
        return userDAO.getFcmTokenByUserId(userId);
    }

    public void saveOrUpdateNotificationData(NotificationRequest notificationRequest) {
        NotificationRequest existingRequest = notificationDAO.getNotificationDataByUserId(notificationRequest.getUserId());

        if (existingRequest != null) {
            if (notificationRequest.getToken() == null || notificationRequest.getToken().isEmpty()) {
                notificationRequest.setToken(existingRequest.getToken());
            }
            notificationDAO.updateNotificationData(notificationRequest);
        } else {
            notificationDAO.saveNotificationData(notificationRequest);
        }
    }
}