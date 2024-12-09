package com.season.semiproject.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.season.semiproject.dao.CourseDAO;
import com.season.semiproject.vo.Course;
import com.season.semiproject.vo.CourseReviews;
import com.season.semiproject.vo.Review;

@RequestMapping("/api/course")
@RestController
public class CourseController {

    @Autowired
    CourseDAO dao;

    @GetMapping
    public List<Course> getAllCourse() {
        return dao.getAllCourses();
    }
    
 // 코스번호로 코스한개 불러오기
    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourseById(@PathVariable Integer courseId) {
       Course course = dao.getCourseById(courseId); // 서비스에 넣을 함수 이름
       if (course != null) {
          return ResponseEntity.ok(course);
       } else {
          return ResponseEntity.notFound().build();
       }
    }
    
    @PostMapping("/byName")
    public Course getCourseByName(@RequestBody Course course) {
    	String courseName = course.courseName;
    	System.out.println(courseName);
    	return dao.byName(courseName);
    }

    @GetMapping("/{courseId}/reviews")
    public ResponseEntity<Map<String, Object>> getCourseReviewsAndRatings(@PathVariable int courseId) {
        List<Review> reviews = dao.getCourseReviews(courseId);
        List<Integer> ratings = dao.getCourseRatings(courseId);
        
        Map<Integer, Integer> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0);
        }
        for (Integer rating : ratings) {
            ratingDistribution.put(rating, ratingDistribution.get(rating) + 1);
        }

        double averageRating = ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviews);
        response.put("ratingDistribution", ratingDistribution);
        response.put("reviewCount", reviews.size());
        response.put("averageRating", Math.round(averageRating * 100) / 100.0);

        return ResponseEntity.ok(response);
    }
   

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, Object>> summarizeCourseReviews(@RequestBody CourseReviews courseReviews) {
        RestTemplate restTemplate = new RestTemplate();
        String pythonApiUrl = "http://localhost:8000/summarize";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("course_name", courseReviews.getCourseName());
        requestBody.put("reviews", courseReviews.getReviews());
        requestBody.put("ratings", courseReviews.getRatings());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);
            return ResponseEntity.ok(responseEntity.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Python 서버 오류 발생: " + e.getMessage()));
        }
    }

    @GetMapping("/{courseId}/summarize")
    public ResponseEntity<Map<String, Object>> getCourseSummary(@PathVariable int courseId) {
        List<Review> reviews = dao.getCourseReviews(courseId);
        List<Integer> ratings = dao.getCourseRatings(courseId);

        double averageRating = ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        RestTemplate restTemplate = new RestTemplate();
        String pythonApiUrl = "http://localhost:8000/summarize";

        List<String> reviewContents = reviews.stream()
                                             .map(Review::getReviewContent)
                                             .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("course_name", dao.getCourseNameById(courseId));
        requestBody.put("reviews", reviewContents);
        requestBody.put("ratings", ratings);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);
            String summary = responseEntity.getBody().get("summary").toString();

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviews);
            response.put("averageRating", Math.round(averageRating * 100) / 100.0);
            response.put("summary", summary);
            response.put("reviewCount", reviews.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Python 서버 오류 발생: " + e.getMessage()));
        }
    }
}
