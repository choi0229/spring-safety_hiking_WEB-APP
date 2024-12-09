package com.season.semiproject.vo;

public class User {

    public String userId;
    public String userPw;
    public String userName;
    public String userNickName;
    public String userEmail;
    public String userAge;
    public String userAddress;
    public String userGender;
    public String userInstitution;  // 기관 정보 필드 추가

    // 기본 생성자
    public User() {}

    // 생성자 (로그인 시 사용)
    public User(String userId, String userPw) {
        this.userId = userId;
        this.userPw = userPw;
    }

    public User(String userId, String userPw, String userName, String userNickName, String userEmail, String userAge, String userAddress, String userGender, String userInstitution) {
        this.userId = userId;
        this.userPw = userPw;
        this.userName = userName;
        this.userNickName = userNickName;
        this.userEmail = userEmail;
        this.userAge = userAge;
        this.userAddress = userAddress;
        this.userGender = userGender;
        this.userInstitution = userInstitution;
    }

    // Getter와 Setter 메서드
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPw() {
        return userPw;
    }

    public void setUserPw(String userPw) {
        this.userPw = userPw;
    }

    public String getUserNickName() {
        return userNickName;
    }

    public void setUserNickName(String userNickName) {
        this.userNickName = userNickName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserAge() {
        return userAge;
    }

    public void setUserAge(String userAge) {
        this.userAge = userAge;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public String getUserGender() {
        return userGender;
    }

    public void setUserGender(String userGender) {
        this.userGender = userGender;
    }

    public String getUserInstitution() {
        return userInstitution;
    }

    public void setUserInstitution(String userInstitution) {
        this.userInstitution = userInstitution;
    }

    @Override
    public String toString() {
        return "User [userId=" + userId +
               ", userPw=" + userPw +
               ", userName=" + userName +
               ", userNickName=" + userNickName +
               ", userEmail=" + userEmail +
               ", userAge=" + userAge +
               ", userAddress=" + userAddress +
               ", userGender=" + userGender +
               ", userInstitution=" + (userInstitution != null ? userInstitution : "일반 사용자") +
               "]";
    }
}
