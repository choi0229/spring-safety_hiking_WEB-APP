package com.season.semiproject.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.dao.UserDAO;
import com.season.semiproject.vo.User;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/api")
@RestController
public class UserController {

    @Autowired
    UserDAO dao;

    @PostMapping("/login")
    public User tryLogin(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        String userPw = payload.get("userPw");
        User user = dao.tryLogin(userId, userPw);
        System.out.println(user);
        return user;
    }
    
 // 회원가입 요청을 처리하는 메서드
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // DAO를 통해 DB에 회원 정보를 저장
            dao.createUser(user);

            // 성공적인 회원가입 응답
            response.put("success", true);
            response.put("userNickname", user.getUserNickName());
            response.put("userGender", user.getUserGender());
            response.put("userId", user.getUserId());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();

            // 예외 발생 시 실패 응답
            response.put("success", false);
            response.put("message", "회원가입에 실패했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ID 중복 체크
    @PostMapping("/checkId")
    public ResponseEntity<Map<String, Object>> checkId(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String userId = payload.get("userId");
        
        boolean isIdTaken = dao.isIdTaken(userId); // DAO에서 ID 중복 체크 메서드 호출
        response.put("success", !isIdTaken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Email 중복 체크
    @PostMapping("/checkEmail")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String userEmail = payload.get("userEmail");
        
        boolean isEmailTaken = dao.isEmailTaken(userEmail); // DAO에서 Email 중복 체크 메서드 호출
        response.put("success", !isEmailTaken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // NickName 중복 체크
    @PostMapping("/checkNickName")
    public ResponseEntity<Map<String, Object>> checkNickName(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        String userNickName = payload.get("userNickName");
        
        boolean isNickNameTaken = dao.isNickNameTaken(userNickName); // DAO에서 NickName 중복 체크 메서드 호출
        response.put("success", !isNickNameTaken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @GetMapping("/userInfo/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
 		User user = dao.findUserById(userId); // 서비스에 넣을 함수 이름
 		if (user != null) {
 			return ResponseEntity.ok(user);
 		} else {
 			return ResponseEntity.notFound().build();
 		}
 	}

}
