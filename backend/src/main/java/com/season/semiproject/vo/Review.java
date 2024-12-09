package com.season.semiproject.vo;

import java.sql.Timestamp;

public class Review {
    public int reviewId; // 리뷰 고유 ID
    public String userId; // 리뷰 작성자 ID
    public String reviewContent; // 리뷰 내용 
    public int rating; // 별점
    public Timestamp date; // 작성 날짜
    public int courseId; // 코스 ID
    public String imagePath; // 이미지 경로
    public String difficulty; // 난이도


    public int getReviewId() {
        return reviewId;
    }

	public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReviewContent() {
        return reviewContent;
    }

    public void setReviewContent(String reviewContent) {
        this.reviewContent = reviewContent;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
