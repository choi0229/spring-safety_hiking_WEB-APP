package com.season.semiproject.vo;

import java.util.Date;

public class Comment {

   public int communityPk;
   public String userNickname;
   public String userId;
   public int commentPk;
   public String commentBody;
   public Date commentRegDate;

   public int getCommunityPk() {
      return communityPk;
   }

   public void setCommunityPk(int communityPk) {
      this.communityPk = communityPk;
   }

   public String getUserNickname() {
      return userNickname;
   }

   public void setUserNickName(String userNickName) {
      this.userNickname = userNickName;
   }

   public int getCommentPk() {
      return commentPk;
   }

   public void setCommentPk(int commentPk) {
      this.commentPk = commentPk;
   }

   public String getCommentBody() {
      return commentBody;
   }

   public void setCommentBody(String commentBody) {
      this.commentBody = commentBody;
   }

   public Date getCommentRegDate() {
      return commentRegDate;
   }

   public void setCommentRegDate(Date commentRegDate) {
      this.commentRegDate = commentRegDate;
   }

   @Override
   public String toString() {
      return "Comment [communityPk=" + communityPk + ", userNickName=" + userNickname + ", commentPk=" + commentPk
            + ", commentBody=" + commentBody + ", commentRegDate=" + commentRegDate + "]";
   }
}
