package com.naho.mobility_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// 네이버가 주는 JSON 데이터 중 우리가 이 클래스에 정의하지 않은 필드는 무시하라는 설정
@JsonIgnoreProperties(ignoreUnknown = true) //응답 JSON의 모든 필드를 다 쓰지 않아도 에러나지 않게 설정
public record NaverGeocodeDto(
    // 네이버 응답 JSON의 "addresses" 라는 키값과 이름이 일치해야함
    List<Address> addresses
) {
    // addresses 키 안에 있는 JSON 객체의 형식을 정의
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String x, // 경도(Longitude)
            String y // 위도(Latitude)
    ) {}
}
