package com.season.semiproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.season.semiproject.vo.Comment;
import com.season.semiproject.vo.Community;
import com.season.semiproject.vo.User;

@Repository
public class CommunityDAO {
   
   @Autowired
   SqlSession session;
   
   // 커뮤니티 리스트 불러오기
   public List<Community> getCommunityList() throws Exception {
      return session.selectList("getCommunityList");
   }
   
   // 안전 리스트 불러오기
   public List<Community> getSafeList() throws Exception {
         return session.selectList("getSafeList");
   }

   
   // 글 한개씩 불러오기
   public Community getCommunityById(int communityPk) {
      return session.selectOne("getCommunityById", communityPk);
   }
   
   
   // 추가
   public void createCommunity(Community vo) {
      session.insert("createCommunity", vo);
   }
   
   
   // 최근 게시글
   public List<Community> getLatestCommunities() {
       return session.selectList("getLatestCommunities"); // 최신 게시글을 가져오는 SQL 쿼리 호출
   }
   
   // 사용자 정보를 조회
   public User getUserById(String userId) {
        return session.selectOne("UserMapper.getUserById", userId); // SQL Mapper 설정 필요
    }

   // 수정
   public int edit(Community vo) {
      return session.update("edit", vo);
   }

   
   // 삭제
   public void deleteCommunity(int CommunityPk) {
      session.delete("deleteCommunity", CommunityPk);
   }


   // 댓글 목록 가져오기
   public List<Community> getCommentsByCommunityId(int CommunityPk) {
      return session.selectList("getCommentsByCommunityId", CommunityPk);
   }

   
   // 댓글 작성
   public void createComment(Comment vo) {
      session.insert("createComment", vo);
   }

   
   // 댓글 삭제
   public void deleteComment(int commentPk) {
      session.delete("deleteComment", commentPk);
   }
   
   
   // 좋아요 수 증가
   public int increaseLikes(Community vo) {
       return session.insert("increaseLikes", vo);
   }
   
   
   // 좋아요 수 감소 (삭제)
   public int decreaseLikes(Community vo) {
       return session.delete("decreaseLikes", vo);
   }

   // 해당 사용자가 게시물에 좋아요를 눌렀는지 확인
   public int isLiked(Community vo) {
       int count = session.selectOne("isLiked", vo);
       return count;
   }
   
   // 특정 커뮤니티의 좋아요 수 가져오기
   public int getLikes(int communityPk) {
       return session.selectOne("getLikes", communityPk);
   }

   public List<Community> getCommunityByCourse(String courseName){
	   return session.selectList("getCommunityByCourse",courseName);
   }
   
}
