package com.season.semiproject.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebaseApp() {
        try {
            // Firebase 서비스 계정 키 JSON 파일의 경로
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            if (serviceAccount == null) {
                throw new IOException("Firebase 서비스 계정 키 파일을 찾을 수 없습니다.");
            }

            // Firebase 옵션 설정
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // FirebaseApp 초기화
            if (FirebaseApp.getApps().isEmpty()) { // 이미 초기화되지 않았는지 확인
                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp이 초기화되었습니다.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Firebase 초기화 중 오류가 발생했습니다.", e);
        }
    }
}
