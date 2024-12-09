package com.season.semiproject.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.season.semiproject.vo.UserProfile;

@Repository
public class UserProfileDAO {

    @Autowired
    SqlSession session;

    private static final String NAMESPACE = "com.season.semiproject.mapper.UserProfileMapper";

    public UserProfileDAO(SqlSession sqlSession) {
        this.session = sqlSession;
    }

    // 프로필 이미지 삽입
    public void insertUserProfile(UserProfile userProfile) {
        session.insert(NAMESPACE + ".insertUserProfile", userProfile);
    }

    // 특정 유저의 프로필 이미지 가져오기
    public String getProfileImage(String userId) {
        String profileImage = session.selectOne(NAMESPACE + ".getProfileImage", userId);
        // 프로필 이미지가 없으면 기본 프로필 이미지 반환
        if (profileImage == null || profileImage.isEmpty()) {
            return "/images/기본프로필.png";  // 기본 프로필 이미지 경로 설정
        }
        return profileImage;
    }

    // 프로필 이미지 업데이트
    public void updateUserProfile(UserProfile userProfile) {
        session.update(NAMESPACE + ".updateUserProfile", userProfile);
    }

    // 특정 유저의 존재 여부 확인
    public String getUserById(String userId) {
        return session.selectOne(NAMESPACE + ".getUserById", userId);
    }
}
