package com.season.semiproject.vo;

import java.math.BigDecimal;

public class PathInfoVO {
	
	Integer pathId;
	String userId;
	String startTime;
	String endTime;
	String totalTime;
	String pathImg;
	BigDecimal totalDistance;
	BigDecimal maxAltitude;
	float avgSpeed;
	
	public Integer getPathId() {
		return pathId;
	}
	public void setPathId(Integer pathId) {
		this.pathId = pathId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public String getTotalTime() {
		return totalTime;
	}
	public void setTotalTime(String totalTime) {
		this.totalTime = totalTime;
	}
	public String getPathImg() {
		return pathImg;
	}
	public void setPathImg(String pathImg) {
		this.pathImg = pathImg;
	}
	public BigDecimal getTotalDistance() {
		return totalDistance;
	}
	public void setTotalDistance(BigDecimal totalDistance) {
		this.totalDistance = totalDistance;
	}
	public BigDecimal getMaxAltitude() {
		return maxAltitude;
	}
	public void setMaxAltitude(BigDecimal maxAltitude) {
		this.maxAltitude = maxAltitude;
	}
	public float getAvgSpeed() {
		return avgSpeed;
	}
	public void setAvgSpeed(float avgSpeed) {
		this.avgSpeed = avgSpeed;
	}
	
	@Override
	public String toString() {
		return "PathInfoVO [pathId=" + pathId + ", userId=" + userId + ", startTime=" + startTime + ", endTime="
				+ endTime + ", totalTime=" + totalTime + ", pathImg=" + pathImg + ", totalDistance=" + totalDistance
				+ ", maxAltitude=" + maxAltitude + ", avgSpeed=" + avgSpeed + "]";
	}
	

}
