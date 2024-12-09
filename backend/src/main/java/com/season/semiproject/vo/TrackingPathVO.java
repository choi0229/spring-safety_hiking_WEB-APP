package com.season.semiproject.vo;

import java.math.BigDecimal;

public class TrackingPathVO {

	Integer trackingPathId;
	Integer pathId;
	BigDecimal latitude;
	BigDecimal longitude;
	BigDecimal altitude;
	float speed;
	float bearing;
	String time;
	
	public Integer getTrackingPathId() {
		return trackingPathId;
	}
	public void setTrackingPathId(Integer trackingPathId) {
		this.trackingPathId = trackingPathId;
	}
	public Integer getPathId() {
		return pathId;
	}
	public void setPathId(Integer pathId) {
		this.pathId = pathId;
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
	public BigDecimal getAltitude() {
		return altitude;
	}
	public void setAltitude(BigDecimal altitude) {
		this.altitude = altitude;
	}
	public float getSpeed() {
		return speed;
	}
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	public float getBearing() {
		return bearing;
	}
	public void setBearing(float bearing) {
		this.bearing = bearing;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
	@Override
	public String toString() {
		return "TrackingPathVO [trackingPathId=" + trackingPathId + ", pathId=" + pathId + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", altitude=" + altitude + ", speed=" + speed + ", bearing=" + bearing
				+ ", time=" + time + "]";
	}
	
	
}
