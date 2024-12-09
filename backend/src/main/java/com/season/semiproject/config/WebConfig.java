package com.season.semiproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 파일을 접근할 수 있도록 경로 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // API 엔드포인트에 대해 CORS 허용
                .allowedOrigins("http://172.30.1.53:8080") // Vue.js 애플리케이션의 URL
                .allowedMethods("POST", "GET", "PUT", "DELETE") // 허용할 HTTP 메서드
                .allowCredentials(true)
        		.allowedHeaders("*")
        		.maxAge(3600);
    }
}
