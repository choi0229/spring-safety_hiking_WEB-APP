package com.season.semiproject.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.dao.TrackingDAO;
import com.season.semiproject.vo.Complaint;
import com.season.semiproject.vo.PathInfoVO;
import com.season.semiproject.vo.TrackingPathVO;

@RestController
@RequestMapping("/api")
public class TrackingController {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
    private TrackingDAO dao;

	@PostMapping("/savePathInfo")
	public int saveTrackingData(@RequestBody PathInfoVO pathInfo) {
		System.out.println("pathInfo = " + pathInfo);
		Integer pathId = dao.insertPathInfo(pathInfo);
		return pathId;
	}

	@PostMapping("/saveTrackingPath")
	public ResponseEntity<String> saveTrackingPath(@RequestBody TrackingPathVO trackingPath) {
		System.out.println("trackingPath = " + trackingPath + "pathId = " + trackingPath.getPathId());
		dao.insertTrackingPath(trackingPath);
		return ResponseEntity.ok("trackingPath inserted successfully");
	}
	
	// 등산 기록 리스트 최신순으로 가져오기
	@GetMapping("/pathList")
	public ResponseEntity<List<PathInfoVO>> pathList() {
		logger.info("info : complaintListRecent");
		
		List<PathInfoVO> pathlist = dao.getPathList();
		return ResponseEntity.ok(pathlist);
	}

	
}
