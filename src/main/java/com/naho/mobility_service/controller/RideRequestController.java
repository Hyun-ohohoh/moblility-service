package com.naho.mobility_service.controller;

import com.naho.mobility_service.dto.RideRequestDto;
import com.naho.mobility_service.service.RideRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RideRequestController {

    private final RideRequestService rideRequestService;

    /**
     * 새로운 탑승 요청을 생성하는 API 엔드포인트입니다.
     * HTTP POST 방식으로 "/api/requests" 주소에 요청이 오면 이 메소드가 실행됩니다.
     * @param requestDto HTTP 요청의 Body에 담겨온 JSON 데이터를 RideRequestDto 객체로 변환하여 받습니다.
     * @return 요청이 성공적으로 생성되었음을 알리는 HTTP 상태 코드 201 (Created)를 반환합니다.
     */
    @PostMapping //Post 요청을 이 메서드와 연결
    public ResponseEntity<Void> createRideRequest(@RequestBody RideRequestDto requestDto){
        rideRequestService.createRideRequest(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
