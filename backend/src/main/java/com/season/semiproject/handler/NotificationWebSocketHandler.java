package com.season.semiproject.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId == null || userId.isEmpty()) {
            logger.warn("WebSocket 연결 실패: userId가 없습니다.");
            session.close();
            return;
        }

        userSessions.put(userId, session);
        logger.info("WebSocket 연결 성공: userId = " + userId + ", 세션 ID = " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            String payload = message.getPayload();
            logger.info("수신된 메시지: " + payload);

            // 사용자별 메시지 브로드캐스트
            broadcastMessage(payload);
        } catch (Exception e) {
            logger.error("WebSocket 메시지 처리 중 오류 발생", e);
        }
    }

    public void sendLocationUpdate(String userId, double latitude, double longitude) throws IOException {
        String message = String.format(
            "{\"type\": \"location_update\", \"userId\": \"%s\", \"latitude\": %f, \"longitude\": %f}",
            userId, latitude, longitude
        );
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }


    public void sendNotificationToAll(String message) throws IOException {
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
                logger.info("WebSocket 메시지 전송: " + message);
            }
        }
    }

    public void sendLocationUpdateToAll(double latitude, double longitude) throws IOException {
        String message = String.format(
            "{\"type\": \"sos_notification\", \"latitude\": %f, \"longitude\": %f}",
            latitude, longitude
        );
        sendNotificationToAll(message);
    }

    private void broadcastMessage(String message) throws IOException {
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        userSessions.remove(userId);
        logger.info("WebSocket 연결 종료: userId = " + userId);
    }

    private String getUserIdFromSession(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        return (String) attributes.get("userId");
    }
}
