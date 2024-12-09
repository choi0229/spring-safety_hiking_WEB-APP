package com.season.semiproject.vo;

//위치 데이터를 담을 요청 객체
public class LocationRequest {
	private double latitude;
	private double longitude;
	private double altitude;
	private float speed;
	private float bearing;
	private String time;
	
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
	public double getAltitude() {
		return altitude;
	}
	public void setAltitude(double altitude) {
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

 
}
