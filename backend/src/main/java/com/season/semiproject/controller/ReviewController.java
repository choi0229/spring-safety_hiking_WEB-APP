package com.season.semiproject.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.season.semiproject.dao.ReviewDAO;
import com.season.semiproject.vo.Review;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewDAO dao;

    // 리뷰 작성
    @PostMapping("/create")
    public ResponseEntity<String> createReview(
        @RequestPart("review") Review review,
        @RequestPart(value = "photos", required = false) List<MultipartFile> photos
    ) {
        // user_id가 null인지 확인
        if (review.getUserId() == null || review.getUserId().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User ID is missing");
        }

        // 이미지 처리 로직
        if (photos != null && !photos.isEmpty()) {
            StringBuilder imagePaths = new StringBuilder();
            String uploadDirectory = "C:/images/";
            for (MultipartFile image : photos) {
                if (!image.isEmpty()) {
                    String fileName = image.getOriginalFilename();
                    String filePath = Paths.get(uploadDirectory, fileName).toString();
                    File directory = new File(uploadDirectory);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File file = new File(filePath);
                    try {
                        image.transferTo(file);
                        imagePaths.append("/images/").append(fileName).append(";");
                    } catch (IOException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
                    }
                }
            }
            review.setImagePath(imagePaths.toString());
        }

        dao.createReview(review);
        return ResponseEntity.ok("리뷰가 성공적으로 작성되었습니다.");
    }

    //리뷰 수정
    @PutMapping("/edit/{reviewId}")
    public ResponseEntity<String> updateReview(@PathVariable int reviewId, @RequestBody Review review) {
        Review existingReview = dao.getReviewById(reviewId); // 기존 리뷰 가져오기
        if (existingReview == null || !existingReview.getUserId().equals(review.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("작성자만 리뷰를 수정할 수 있습니다.");
        }

        review.setReviewId(reviewId);
        dao.updateReview(review);
        return ResponseEntity.ok("리뷰가 성공적으로 수정되었습니다.");
    }

    // 리뷰 삭제
    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable int reviewId, @RequestParam String userId) {
        Review existingReview = dao.getReviewById(reviewId);
        
        if (existingReview == null) {
            System.out.println("해당 리뷰가 존재하지 않습니다. reviewId: " + reviewId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 리뷰가 존재하지 않습니다.");
        }
        
        if (!existingReview.getUserId().equals(userId)) {
            System.out.println("작성자만 삭제할 수 있습니다. userId: " + userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("작성자만 리뷰를 삭제할 수 있습니다.");
        }

        dao.deleteReview(reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }

    // 리뷰 검색
    @GetMapping("/search")
    public ResponseEntity<List<Review>> searchReviews(@RequestParam int courseId, @RequestParam String query) {
        List<Review> reviews = dao.searchReviews(courseId, query);
        return ResponseEntity.ok(reviews);
    }
}
