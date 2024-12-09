package com.season.semiproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.season.semiproject.vo.Course;
import com.season.semiproject.vo.Review;

@Repository
public class CourseDAO {

    @Autowired
    SqlSession session;
    
    public Course getCourseById(Integer courseId) {
        return session.selectOne("getCourse", courseId);
     }

    // 코스 리스트
    public List<Course> getAllCourses() {
        return session.selectList("getAllCourses");
    }
    
    public Course byName(String courseName) {
    	return session.selectOne("getCourseByName",courseName);
    }

    // 리뷰 요약
    public List<Review> getCourseReviews(int courseId) {
        return session.selectList("getCourseReviews", courseId);
    }

    // 별점
    public List<Integer> getCourseRatings(int courseId) {
        return session.selectList("getCourseRatings", courseId);
    }

    // 리뷰
    public String getCourseNameById(int courseId) {
        return session.selectOne("getCourseNameById", courseId);
    }
    
    // 검색
    public List<Review> searchCourseReviews(int courseId, String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("courseId", courseId);
        params.put("query", query);
        return session.selectList("searchCourseReviews", params);
    }

}
