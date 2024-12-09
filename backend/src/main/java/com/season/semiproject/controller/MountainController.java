package com.season.semiproject.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.season.semiproject.dao.MountainDAO;
import com.season.semiproject.vo.Mountain;

@RequestMapping("/api/mountains")
@RestController
public class MountainController {

	@Autowired
    MountainDAO mountainDAO;

    // 모든 산 목록 조회
    @GetMapping
    public List<Mountain> getAllMountains() {
        return mountainDAO.getAllMountains();
    }

    // 특정 ID의 산 정보 조회
    @GetMapping("/{mountainId}")
    public Mountain getMountainById(@PathVariable int mountainId) {
        return mountainDAO.getMountainById(mountainId);
    }

}
