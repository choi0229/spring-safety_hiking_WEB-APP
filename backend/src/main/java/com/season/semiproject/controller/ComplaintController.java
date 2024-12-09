package com.season.semiproject.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.season.semiproject.dao.ComplaintDAO;
import com.season.semiproject.vo.Complaint;
import com.season.semiproject.vo.Processing;

@RestController
@RequestMapping("/api/complaint")
public class ComplaintController {
Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	ComplaintDAO dao;
	
	@GetMapping("/list")
	public ResponseEntity<List<Complaint>> complaintList() {
		logger.info("info : complaintList");
		
		List<Complaint> memberlist = dao.getComplaintList();
		
		return ResponseEntity.ok(memberlist);
//		if (memberlist != null) {
//			return ResponseEntity.ok(memberlist);
//		} else {
//			return ResponseEntity.status(HttpStatus.CONFLICT).build();
//		}
	}
	
	// 민원글 리스트 최신순으로 가져오기
	@GetMapping("/listRecent")
	public ResponseEntity<List<Complaint>> complaintListRecent() {
		logger.info("info : complaintListRecent");
		
		List<Complaint> memberlist = dao.getRecentComplaintList();
		
		return ResponseEntity.ok(memberlist);
	}
	
	// 담당에 따른 민원 리스트 최신순으로 가져오기
		@GetMapping("/myList/{institution}")
		public ResponseEntity<List<Complaint>> complaintListByInst(@PathVariable String institution) {
			List<Complaint> managingList = dao.getComplaintListByInst(institution);
			
			return ResponseEntity.ok(managingList);
		}
	
	// 민원글 등록
	@PostMapping("/insert")
	public ResponseEntity<String> insertComplaint(@RequestBody Complaint complaint) {
		System.out.println("complaint = " + complaint);
		dao.insertComplaint(complaint);
		return ResponseEntity.ok("Complaint inserted successfully");
	}
	
	// 민원 - 이미지 업로드
	@PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String folderPath = "C:/Users/ASUS/Final_project/semiproject/frontend/public/images";
        String fileName = file.getOriginalFilename();
        System.out.println("사진들어옴");
        File saveFile = new File(folderPath + fileName);
        try {
            // 파일을 지정된 경로에 저장
            file.transferTo(saveFile);
            
            // 저장된 파일 경로를 반환 (DB에 저장할 경로)
            String imagePath = "/images/" + fileName;
            return ResponseEntity.ok(imagePath);  // 클라이언트로 이미지 경로 반환
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("File upload failed");
        }
    }
	
	// 민원 번호로 민원글 한개 불러오기
	@GetMapping("/one/{complaintNo}")
	public ResponseEntity<Complaint> getComplaint(@PathVariable Integer complaintNo) {
		logger.info("info : complaintNo" + complaintNo);
		Complaint complaint = dao.getComplaintByNo(complaintNo); // 서비스에 넣을 함수 이름
		if (complaint != null) {
			return ResponseEntity.ok(complaint);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	// 민원 번호로 민원글 삭제하기
	@DeleteMapping("/delete/{complaintNo}")
	public ResponseEntity<String> deleteComplaint(@PathVariable Integer complaintNo) {
		try {
			logger.info("info : 글삭제완료, 글번호 : " + complaintNo);
			dao.deleteComplaintByNo(complaintNo);
			return ResponseEntity.ok("민원글이 성공적으로 삭제되었습니다.");
		} catch (Exception e) {
			System.out.println(e);
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}
	
	// 민원 번호로 민원글 수정하기
	@PatchMapping("/update/{complaintNo}")
	public ResponseEntity<String> updateComplaint(@PathVariable Integer complaintNo, @RequestBody Complaint complaint) {
		try {
			logger.info("info : 글수정완료, 글번호 : " + complaintNo);
			System.out.println("complaint = " + complaint);
			dao.updateComplaint(complaint);
			return ResponseEntity.ok("게시글이 성공적으로 수정되었습니다.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}
	
	// 민원 번호로 민원처리정보 한개 불러오기
		@GetMapping("/processing/{processingComplaintNo}")
		public ResponseEntity<Processing> getProcessing(@PathVariable Integer processingComplaintNo) {
			logger.info("info : 처리할complaintNo" + processingComplaintNo);
			Processing processing = dao.getProcessingByNo(processingComplaintNo); // 서비스에 넣을 함수 이름
			if (processing != null) {
				return ResponseEntity.ok(processing);
			} else {
				return ResponseEntity.notFound().build();
			}
		}

	// 민원처리정보 등록
	@PostMapping("/insertProcessing")
	public ResponseEntity<String> insertProcessing(@RequestBody Processing processing) {
		System.out.println("processing = " + processing);
		dao.insertProcessing(processing);
		return ResponseEntity.ok("processing is inserted successfully");
	}
	
	// 민원 번호로 민원글 수정하기
		@PatchMapping("/updateProcessing/{processingComplaintNo}")
		public ResponseEntity<String> updateProcessing(@PathVariable Integer processingComplaintNo, @RequestBody Processing processing) {
			try {
				logger.info("info : 글수정완료, 글번호 : " + processingComplaintNo);
				System.out.println("processing = " + processing);
				dao.updateProcessing(processing);
				return ResponseEntity.ok("게시글이 성공적으로 수정되었습니다.");
			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}
		}
		
		@GetMapping("/rank")
		public List<Complaint> getComplaintListRank() {
			return dao.getComplaintListRank();
		}
		
		// 민원 글쓴이로 민원글 불러오기
		@GetMapping("/mine/{userId}")
		public ResponseEntity<List<Complaint>> getMyComplaint(@PathVariable String userId) {
			logger.info("info : userId" + userId);
			List<Complaint> myComplaintList = dao.getComplaintByUserId(userId); // 서비스에 넣을 함수 이름
			if (userId != null) {
				return ResponseEntity.ok(myComplaintList);
			} else {
				return ResponseEntity.notFound().build();
			}
		}


}
