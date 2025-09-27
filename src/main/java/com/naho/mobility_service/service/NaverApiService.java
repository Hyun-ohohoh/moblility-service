package com.naho.mobility_service.service;

import com.naho.mobility_service.dto.NaverGeocodeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class NaverApiService {

    private final RestTemplate restTemplate;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    public NaverGeocodeDto geocode(String address) {

        // 1. API 요청 주소(URL) 생성 ("어디로 전화걸지?")
        URI uri = UriComponentsBuilder
                .fromUriString("https://naveropenapi.apigw.ntruss.com") // 네이버 openAPI 서버 주소
                .path("/map-geocode/v2/geocode") // 기능 경로
                .queryParam("query", address) // "이 주소로 검색해줘"라고 파라미터 추가
                .encode()
                .build()
                .toUri();

        // 2. HTTP 요청 해더에 비밀번호 설정 ("내가 누군지 밝히기")
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
        headers.set("X-NCP-APIGW-API-KEY", clientSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 3. RestTemplate을 사용해 GET 방식으로 API 호출
        ResponseEntity<NaverGeocodeDto> response = restTemplate.exchange(uri, HttpMethod.GET, entity, NaverGeocodeDto.class);

        return response.getBody();
    }
}
