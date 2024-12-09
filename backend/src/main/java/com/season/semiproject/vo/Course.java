package com.season.semiproject.vo;

public class Course {
	
	public int courseId;
	public String courseName;
	public String mountainName;
	public double courseRate;
	public String courseImage;
	public String courseVideo;
	public String courseLocation;
	public String courseContent;
	public String courseLevel;
	public String distance;
	public String duration;
	public double courseLat;
	public double courseLon;
	
	public int getCourseId() {
		return courseId;
	}
	public void setCourseId(int courseId) {
		this.courseId = courseId;
	}
	public String getCourseName() {
		return courseName;
	}
	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	public String getMountainName() {
		return mountainName;
	}
	public void setMountainName(String mountainName) {
		this.mountainName = mountainName;
	}
	public double getCourseRate() {
		return courseRate;
	}
	public void setCourseRate(double courseRate) {
		this.courseRate = courseRate;
	}
	public String getCourseImage() {
		return courseImage;
	}
	public void setCourseImage(String courseImage) {
		this.courseImage = courseImage;
	}
	public String getCourseVideo() {
		return courseVideo;
	}
	public void setCourseVideo(String courseVideo) {
		this.courseVideo = courseVideo;
	}
	public String getCourseLocation() {
		return courseLocation;
	}
	public void setCourseLocation(String courseLocation) {
		this.courseLocation = courseLocation;
	}
	public String getCourseContent() {
		return courseContent;
	}
	public void setCourseContent(String courseContent) {
		this.courseContent = courseContent;
	}
	public String getDistance() {
		return distance;
	}
	public void setDistance(String distance) {
		this.distance = distance;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public double getCourseLat() {
		return courseLat;
	}
	public void setCourseLat(double courseLat) {
		this.courseLat = courseLat;
	}
	public double getCourseLon() {
		return courseLon;
	}
	public void setCourseLon(double courseLon) {
		this.courseLon = courseLon;
	}
	@Override
	public String toString() {
		return "Course [courseId=" + courseId + ", courseName=" + courseName + ", mountainName=" + mountainName
				+ ", courseRate=" + courseRate + ", courseImage=" + courseImage + ", courseVideo=" + courseVideo
				+ ", courseLocation=" + courseLocation + ", courseContent=" + courseContent + ", distance=" + distance
				+ ", duration=" + duration + ", courseLat=" + courseLat + ", courseLon=" + courseLon + "]";
	}
	
	
	
}