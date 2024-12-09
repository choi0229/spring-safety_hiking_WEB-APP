package com.season.semiproject.vo;

public class Count {

	int contentId;
	int countNum;
	
	public int getContentId() {
		return contentId;
	}
	public void setContentId(int contentId) {
		this.contentId = contentId;
	}
	public int getCountNum() {
		return countNum;
	}
	public void setCountNum(int countNum) {
		this.countNum = countNum;
	}
	@Override
	public String toString() {
		return "Count [contentId=" + contentId + ", countNum=" + countNum + "]";
	}
	
	
	
	
	
}
