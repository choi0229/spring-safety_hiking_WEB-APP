package com.season.semiproject.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.season.semiproject.dao.UserProfileDAO;
import com.season.semiproject.vo.UserProfile;

@RestController
public class UserProfileController {

	private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
	
    @Autowired
    private UserProfileDAO userProfileDAO;

    // 기본 프로필 이미지 경로 설정
    private static final String DEFAULT_PROFILE_IMAGE = "/images/기본프로필.png";
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    // 프로필 사진 업로드 핸들러
    @PostMapping("/api/uploadprofile")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            @RequestParam("userId") String userId) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("error", "파일이 업로드되지 않았습니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // 사용자별 디렉토리 생성
            String userDir = UPLOAD_DIR + userId + "/";
            File userDirectory = new File(userDir);
            if (!userDirectory.exists()) {
                userDirectory.mkdirs();
            }

            // 고유 파일 이름 생성 및 저장 경로 설정
            String fileName = userId + "_" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String filePath = Paths.get(userDir, fileName).toAbsolutePath().toString();
            logger.info("File path: {}", filePath);  // Logger 사용

            // 파일을 서버에 저장
            File dest = new File(filePath);
            file.transferTo(dest);

            // 사용자별 서버의 이미지 URL 생성
            String fileUrl = "http://localhost:9000/uploads/" + userId + "/" + fileName;
            logger.info("File URL: {}", fileUrl);  // Logger 사용
            
            // DB에 프로필 이미지 경로 저장 또는 업데이트
            UserProfile existingProfile = new UserProfile(userId, fileUrl);
            String existingImage = userProfileDAO.getProfileImage(userId);

            if (existingImage == null) {
                userProfileDAO.insertUserProfile(existingProfile); // 새로 입력
            } else {
                userProfileDAO.updateUserProfile(existingProfile); // 기존 이미지 업데이트
            }

            // 성공적으로 업로드된 이미지 URL 반환
            response.put("message", "프로필 사진이 성공적으로 업로드되었습니다.");
            response.put("imageUrl", fileUrl);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "파일 업로드 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 프로필 이미지 가져오는 API
    @GetMapping("/api/getProfileImage")
    public ResponseEntity<Map<String, Object>> getProfileImage(@RequestParam("userId") String userId) {
        Map<String, Object> response = new HashMap<>();
        String profileImage = userProfileDAO.getProfileImage(userId);

        // 프로필 이미지가 없으면 기본 이미지 반환
        if (profileImage == null || profileImage.isEmpty()) {
            response.put("imageUrl", "http://localhost:9000" + DEFAULT_PROFILE_IMAGE);
        } else {
            response.put("imageUrl", profileImage);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
