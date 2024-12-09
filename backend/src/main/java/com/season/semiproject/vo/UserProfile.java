package com.season.semiproject.vo;

public class UserProfile {
    private int profileId;
    private String userId;
    private String profileImage;

    // 기본 생성자
    public UserProfile() {}

    // 매개변수 생성자
    public UserProfile(String userId, String profileImage) {
        this.userId = userId;
        this.profileImage = profileImage;
    }

    // Getter 및 Setter 메서드
    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUsereId(String userId) {
        this.userId = userId;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    @Override
    public String toString() {
        return "UserProfile [profileId=" + profileId + ", userId=" + userId + ", profileImage=" + profileImage + "]";
    }
}
