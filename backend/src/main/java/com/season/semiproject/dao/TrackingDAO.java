package com.season.semiproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.season.semiproject.vo.PathInfoVO;
import com.season.semiproject.vo.TrackingPathVO;

@Component
public class TrackingDAO {

	@Autowired
	SqlSession session;

	// 경로 정보 등록
	public Integer insertPathInfo(PathInfoVO pathInfo) {
		session.insert("insertPathInfo", pathInfo);	
		System.out.println("pathinfo inserted : " + pathInfo);
		return pathInfo.getPathId();
	}
	
	// 경로 자세한 위경도 정보 등록
	public void insertTrackingPath(TrackingPathVO trackingPath) {
		System.out.println("TrackingPath inserted : " + trackingPath);
		session.insert("insertTrackingPath", trackingPath);
	}

	public List<PathInfoVO> getPathList() {
		return session.selectList("getPathList");
	}

}
