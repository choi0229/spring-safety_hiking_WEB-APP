package com.season.semiproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.season.semiproject.vo.NotificationRequest;

@Repository
public class NotificationDAO {

    @Autowired
    private SqlSession sqlSession;

    public void saveNotificationData(NotificationRequest request) {
        sqlSession.insert("saveNotificationData", request);
    }

    public void updateNotificationData(NotificationRequest request) {
        sqlSession.update("updateNotificationData", request);
    }
    public NotificationRequest getNotificationDataByUserId(String userId) {
        return sqlSession.selectOne("getNotificationDataByUserId", userId);
    }
    // 모든 FCM 토큰을 조회하는 메서드 추가
    public List<String> getAllFcmTokens() {
        return sqlSession.selectList("getAllFcmTokens");
    }
}
