package com.season.semiproject.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.dao.MentorContentDAO;
import com.season.semiproject.vo.Count;
import com.season.semiproject.vo.MentorContent;

@RestController
@RequestMapping("/api/mentordetail")
public class MentorContentController {

    @Autowired
    private MentorContentDAO mentorContentDAO;

    @PostMapping("/create")
    public void createMentorContent(@RequestBody MentorContent content) {
    	System.out.println(content);
        mentorContentDAO.insertMentorContent(content);
    }
    
    @GetMapping("/list")
    public List<MentorContent> getMentorContentList() {
        return mentorContentDAO.getMentorContentList();
    }
    
    @GetMapping("/count")
    public List<Count> getCount() {
    	return mentorContentDAO.getCount();
    }
    
}
