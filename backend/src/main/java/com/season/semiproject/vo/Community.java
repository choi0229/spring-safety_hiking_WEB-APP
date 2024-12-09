package com.season.semiproject.vo;

import java.util.Date;
import java.util.List;

public class Community {

   public int communityPk;
   public String userId;
   public String userNickName;
   public String communityUrl;
   public String communityTitle;
   public String communityBody;
   public String communityRegDate;
   public String courseName;
   public Double latitude;
   public Double longitude;
   public int commentPk;
   public String commentBody;
   public Date commentRegDate;
   public int likes; // 좋아요 수
   public double averageRating; // 평균 별점
   public List<String> hashtags; // 해시태그 목록
   public int publicState;
   public int commentCount;
   public int safeState;
   
   
   
   public int getSafeState() {
	return safeState;
}

public void setSafeState(int safeState) {
	this.safeState = safeState;
}

public String getCourseName() {
	return courseName;
}

public void setCourseName(String courseName) {
	this.courseName = courseName;
}

public int getCommentCount() {
      return commentCount;
   }

   public void setCommentCount(int commentCount) {
      this.commentCount = commentCount;
   }

   public int getPublicState() {
      return publicState;
   }

   public void setPublicState(int publicState) {
      this.publicState = publicState;
   }

   public int getCommunityPk() {
      return communityPk;
   }

   public void setCommunityPk(int communityPk) {
      this.communityPk = communityPk;
   }

   public String getUserId() {
      return userId;
   }

   public void setUserId(String userId) {
      this.userId = userId;
   }

   public String getUserNickName() {
      return userNickName;
   }

   public void setUserNickName(String userNickName) {
      this.userNickName = userNickName;
   }

   public String getCommunityUrl() {
      return communityUrl;
   }

   public void setCommunityUrl(String communityUrl) {
      this.communityUrl = communityUrl;
   }

   public String getCommunityTitle() {
      return communityTitle;
   }

   public void setCommunityTitle(String communityTitle) {
      this.communityTitle = communityTitle;
   }

   public String getCommunityBody() {
      return communityBody;
   }

   public void setCommunityBody(String communityBody) {
      this.communityBody = communityBody;
   }

   public String getCommunityRegDate() {
      return communityRegDate;
   }

   public void setCommunityRegDate(String communityRegDate) {
      this.communityRegDate = communityRegDate;
   }

   public Double getLatitude() {
      return latitude;
   }

   public void setLatitude(Double latitude) {
      this.latitude = latitude;
   }

   public Double getLongitude() {
      return longitude;
   }

   public void setLongitude(Double longitude) {
      this.longitude = longitude;
   }

   public int getCommentPk() {
      return commentPk;
   }

   public void setCommentPk(int commentPk) {
      this.commentPk = commentPk;
   }

   public String getCommentBody() {
      return commentBody;
   }

   public void setCommentBody(String commentBody) {
      this.commentBody = commentBody;
   }

   public Date getCommentRegDate() {
      return commentRegDate;
   }

   public void setCommentRegDate(Date commentRegDate) {
      this.commentRegDate = commentRegDate;
   }

   public int getLikes() {
      return likes;
   }

   public void setLikes(int likes) {
      this.likes = likes;
   }

   public double getAverageRating() {
      return averageRating;
   }

   public void setAverageRating(double averageRating) {
      this.averageRating = averageRating;
   }

   public List<String> getHashtags() {
      return hashtags;
   }

   public void setHashtags(List<String> hashtags) {
      this.hashtags = hashtags;
   }

   @Override
public String toString() {
	return "Community [communityPk=" + communityPk + ", userId=" + userId + ", userNickName=" + userNickName
			+ ", communityUrl=" + communityUrl + ", communityTitle=" + communityTitle + ", communityBody="
			+ communityBody + ", communityRegDate=" + communityRegDate + ", courseName=" + courseName + ", latitude="
			+ latitude + ", longitude=" + longitude + ", commentPk=" + commentPk + ", commentBody=" + commentBody
			+ ", commentRegDate=" + commentRegDate + ", likes=" + likes + ", averageRating=" + averageRating
			+ ", hashtags=" + hashtags + ", publicState=" + publicState + ", commentCount=" + commentCount
			+ ", safeState=" + safeState + "]";
}
}
