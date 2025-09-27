package com.naho.mobility_service.dto;

import com.naho.mobility_service.domain.Region;

import java.time.LocalDateTime;

public record RideRequestDto(
        Region region, // 목적지 대권역
        String destinationAddess, //상세 목적지 주소
        LocalDateTime requestedTime //희망 출발 시간

) {


}
