package com.season.semiproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.season.semiproject.vo.Complaint;
import com.season.semiproject.vo.Processing;

@Component
public class ComplaintDAO {
	
	@Autowired
	SqlSession session;
	
	// 민원글들 정보를 DB에서 꺼내서 보여주기
	public List<Complaint> getComplaintList() {
		return session.selectList("getComplaintList");
	}

	public void insertComplaint(Complaint complaint) {
		session.insert("insertComplaint", complaint);	
	}

	public Complaint getComplaintByNo(Integer complaintNo) {
		return session.selectOne("getComplaint", complaintNo);
	}

	public void deleteComplaintByNo(Integer complaintNo) {
		session.delete("deleteComplaintByNo", complaintNo);
	}

	public void updateComplaint(Complaint complaint) {
		session.update("updateComplaint", complaint);
	}

	public Processing getProcessingByNo(Integer processingComplaintNo) {
		return session.selectOne("getProcessing", processingComplaintNo);
	}

	public void insertProcessing(Processing processing) {
		session.insert("insertProcessing", processing);
		
	}

	public void updateProcessing(Processing processing) {
		session.update("updateProcessing", processing);
	}

	public List<Complaint> getRecentComplaintList() {
		return session.selectList("getRecentComplaintList");
	}
	
	public List<Complaint> getComplaintListRank() {
		return session.selectList("getComplaintListRank");
	}
	
	public List<Complaint> getComplaintByUserId(String userId) {
		return session.selectList("getComplaintByUserId", userId);
	}
	
	public List<Complaint> getComplaintListByInst(String institution) {
		return session.selectList("getComplaintListByInst", institution);
	}

	
}
