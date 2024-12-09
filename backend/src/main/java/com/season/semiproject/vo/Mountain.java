package com.season.semiproject.vo;

public class Mountain {
	
	public int mountainId;
	public String mountainName;
	public String mountainImage;
	public String mountainLocation;
	public String mountainContent;
	public double mountainLat;
	public double mountainLon;
	
	public int getMountainId() {
		return mountainId;
	}
	public void setMountainId(int mountainId) {
		this.mountainId = mountainId;
	}
	public String getMountainName() {
		return mountainName;
	}
	public void setMountainName(String mountainName) {
		this.mountainName = mountainName;
	}
	public String getMountainImage() {
		return mountainImage;
	}
	public void setMountainImage(String mountainImage) {
		this.mountainImage = mountainImage;
	}
	public String getMountainLocation() {
		return mountainLocation;
	}
	public void setMountainLocation(String mountainLocation) {
		this.mountainLocation = mountainLocation;
	}
	public String getMountainContent() {
		return mountainContent;
	}
	public void setMountainContent(String mountainContent) {
		this.mountainContent = mountainContent;
	}
	public double getMountainLat() {
		return mountainLat;
	}
	public void setMountainLat(double mountainLat) {
		this.mountainLat = mountainLat;
	}
	public double getMountainLon() {
		return mountainLon;
	}
	public void setMountainLon(double mountainLon) {
		this.mountainLon = mountainLon;
	}
	@Override
	public String toString() {
		return "Mountain [mountainId=" + mountainId + ", mountainName=" + mountainName + ", mountainImage="
				+ mountainImage + ", mountainLocation=" + mountainLocation + ", mountainContent=" + mountainContent
				+ ", mountainLat=" + mountainLat + ", mountainLon=" + mountainLon + "]";
	}
	
	

	
	
	
}
