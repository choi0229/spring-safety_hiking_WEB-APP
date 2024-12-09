package com.season.semiproject.vo;

import java.math.BigDecimal;

public class Complaint {
	Integer complaintNo;
	String complaintTitle;
	String complaintContent;
	String complaintType;
	String complaintState;
	String createdAt;
	String updatedAt;
	String complaintImg;
	String userId;
	String mountainName;
	BigDecimal latitude;
	BigDecimal longitude;
	int countComplaint;
	String institution;
	
	public Integer getComplaintNo() {
		return complaintNo;
	}
	public void setComplaintNo(Integer complaintNo) {
		this.complaintNo = complaintNo;
	}
	public String getComplaintTitle() {
		return complaintTitle;
	}
	public void setComplaintTitle(String complaintTitle) {
		this.complaintTitle = complaintTitle;
	}
	public String getComplaintContent() {
		return complaintContent;
	}
	public void setComplaintContent(String complaintContent) {
		this.complaintContent = complaintContent;
	}
	public String getComplaintType() {
		return complaintType;
	}
	public void setComplaintType(String complaintType) {
		this.complaintType = complaintType;
	}
	public String getComplaintState() {
		return complaintState;
	}
	public void setComplaintState(String complaintState) {
		this.complaintState = complaintState;
	}
	public String getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	public String getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}
	public String getComplaintImg() {
		return complaintImg;
	}
	public void setComplaintImg(String complaintImg) {
		this.complaintImg = complaintImg;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getMountainName() {
		return mountainName;
	}
	public void setMountainName(String mountainName) {
		this.mountainName = mountainName;
	}
	public BigDecimal getLatitude() {
		return latitude;
	}
	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}
	public BigDecimal getLongitude() {
		return longitude;
	}
	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}
	public int getCountComplaint() {
		return countComplaint;
	}
	public void setCountComplaint(int countComplaint) {
		this.countComplaint = countComplaint;
	}
	public String getInstitution() {
		return institution;
	}
	public void setInstitution(String institution) {
		this.institution = institution;
	}
	@Override
	public String toString() {
		return "Complaint [complaintNo=" + complaintNo + ", complaintTitle=" + complaintTitle + ", complaintContent="
				+ complaintContent + ", complaintType=" + complaintType + ", complaintState=" + complaintState
				+ ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", complaintImg=" + complaintImg
				+ ", userId=" + userId + ", mountainName=" + mountainName + ", latitude=" + latitude + ", longitude="
				+ longitude + ", countComplaint=" + countComplaint + ", institution=" + institution + "]";
	}
	
	
}
