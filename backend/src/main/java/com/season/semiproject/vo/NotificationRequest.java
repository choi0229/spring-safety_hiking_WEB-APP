package com.season.semiproject.vo;

public class NotificationRequest {
    private String token;
    private String title;
    private String body;
   
    private String userId;
    private String userNickName;
    
    double latitude;
    double longitude;
    
    private boolean isToAll;  // 모든 사용자에게 알림인지 여부

    public String getUserId() {
        return userId;
    }

    public String getUserNickName() {
        return userNickName;
    }

   
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
   
    
    public boolean isToAll() {
        return isToAll;
    }

    public void setToAll(boolean toAll) {
        isToAll = toAll;
    }
    
   @Override
   public String toString() {
      return "NotificationRequest [token=" + token + ", title=" + title + ", body=" + body
            +  ", userId=" + userId + "]";
   }

     // Getters and Setters
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
