package com.season.semiproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.season.semiproject.vo.Count;
import com.season.semiproject.vo.MentorContent;

@Component
public class MentorContentDAO {

    @Autowired
    private SqlSession sqlSession;
    
    public void insertMentorContent(MentorContent content) {
        sqlSession.insert("mapper-mentordetail.insertMentorContent", content);
    }

    public List<MentorContent> getMentorContentList() {
        return sqlSession.selectList("mapper-mentordetail.getMentorContentList");
    }
    
    public List<Count> getCount() {
    	return sqlSession.selectList("getCount"); 
    }
}
