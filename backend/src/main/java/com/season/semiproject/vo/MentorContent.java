package com.season.semiproject.vo;

import java.sql.Timestamp;

public class MentorContent {
    private int contentId;
    private String title;
    private String contentDetail;
    private String contentHashtags;
    private String userNickname;
    private String userAddress;
    private Timestamp createdAt;
    private int totalPerson;
    private int numApplicants;
    
    
	public int getNumApplicants() {
		return numApplicants;
	}
	public void setNumApplicants(int numApplicants) {
		this.numApplicants = numApplicants;
	}
	public int getContentId() {
		return contentId;
	}
	public void setContentId(int contentId) {
		this.contentId = contentId;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContentDetail() {
		return contentDetail;
	}
	public void setContentDetail(String contentDetail) {
		this.contentDetail = contentDetail;
	}
	public String getContentHashtags() {
		return contentHashtags;
	}
	public void setContentHashtags(String contentHashtags) {
		this.contentHashtags = contentHashtags;
	}
	public String getUserNickname() {
		return userNickname;
	}
	public void setUserNickname(String userNickname) {
		this.userNickname = userNickname;
	}
	public String getUserAddress() {
		return userAddress;
	}
	public void setUserAddress(String userAddress) {
		this.userAddress = userAddress;
	}
	public Timestamp getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
	public int getTotalPerson() {
		return totalPerson;
	}
	public void setTotalPerson(int totalPerson) {
		this.totalPerson = totalPerson;
	}
	
	@Override
	public String toString() {
		return "MentorContent [contentId=" + contentId + ", title=" + title + ", contentDetail=" + contentDetail
				+ ", contentHashtags=" + contentHashtags + ", userNickname=" + userNickname + ", userAddress="
				+ userAddress + ", createdAt=" + createdAt + ", totalPerson=" + totalPerson + ", numApplicants="
				+ numApplicants + "]";
	}
	
	
    
    
    
   
	
}
