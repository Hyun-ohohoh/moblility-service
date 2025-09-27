package com.naho.mobility_service.controller;

import com.naho.mobility_service.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final MatchingService matchingService;

    @GetMapping("/test-matching")
    public String testMatching(){
        System.out.println("테스트 API 호출! 강제로 매칭 로직을 실행합니다.");
        matchingService.runConsolidatedMatching();
        return "매칭 로직 실행.";
    }
}
