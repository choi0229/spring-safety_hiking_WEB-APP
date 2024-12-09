package com.season.semiproject.vo;

public class Processing {
	Integer processingNo;
	String processingContent;
	String processor;
	Integer processingComplaintNo;
	String createdAt;
	String processingImg;
	
	public Integer getProcessingNo() {
		return processingNo;
	}
	public void setProcessingNo(Integer processingNo) {
		this.processingNo = processingNo;
	}
	public String getProcessingContent() {
		return processingContent;
	}
	public void setProcessingContent(String processingContent) {
		this.processingContent = processingContent;
	}
	public String getProcessor() {
		return processor;
	}
	public void setProcessor(String processor) {
		this.processor = processor;
	}
	public Integer getProcessingComplaintNo() {
		return processingComplaintNo;
	}
	public void setProcessingComplaintNo(Integer processingComplaintNo) {
		this.processingComplaintNo = processingComplaintNo;
	}
	public String getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	public String getProcessingImg() {
		return processingImg;
	}
	public void setProcessingImg(String processingImg) {
		this.processingImg = processingImg;
	}
	
	@Override
	public String toString() {
		return "Processing [processingNo=" + processingNo + ", processingContent=" + processingContent + ", processor="
				+ processor + ", processingComplaintNo=" + processingComplaintNo + ", createdAt=" + createdAt
				+ ", processingImg=" + processingImg + "]";
	}
	
}
