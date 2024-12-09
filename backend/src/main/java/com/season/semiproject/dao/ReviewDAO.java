package com.season.semiproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.season.semiproject.vo.Review;

@Repository
public class ReviewDAO {

    @Autowired
    SqlSession session;

    // 리뷰 작성
    public void createReview(Review review) {
        session.insert("mapper-review.insertReview", review);
    }

    // 리뷰 수정
    public void updateReview(Review review) {
        session.update("mapper-review.updateReview", review);
    }

    // 리뷰 삭제
    public void deleteReview(int reviewId) {
        session.delete("mapper-review.deleteReview", reviewId);
    }

    // 리뷰 검색
    public List<Review> searchReviews(int courseId, String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("courseId", courseId);
        params.put("query", query);
        return session.selectList("mapper-review.searchReviews", params);
    }
    
 // 특정 리뷰 가져오기
    public Review getReviewById(int reviewId) {
        return session.selectOne("mapper-review.getReviewById", reviewId);
    }
}
