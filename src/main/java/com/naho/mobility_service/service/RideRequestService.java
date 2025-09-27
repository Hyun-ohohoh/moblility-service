package com.naho.mobility_service.service;

import com.naho.mobility_service.domain.RideRequest;
import com.naho.mobility_service.dto.NaverGeocodeDto;
import com.naho.mobility_service.dto.RideRequestDto;
import com.naho.mobility_service.repository.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final NaverApiService naverApiService;

    @Transactional
    public void createRideRequest(RideRequestDto requestDto){
        // 1. '통신 전문가'에게 주소를 넘겨주고, 좌표로 변환해달라고 시킴
        NaverGeocodeDto geocodeResult = naverApiService.geocode(requestDto.destinationAddess());

        // 2. 결과를 확인하고 좌표를 추출
        // 네이버가 주소를 못 찾아서 결과를 안 줬을 경우를 대비한 안전장치
        if(geocodeResult == null || geocodeResult.addresses() == null || geocodeResult.addresses().isEmpty()){
            // 실제 서비스라면 여기서 특정 예외를 발생시켜 사용자에게 "잘못된 주소"라고 알려줘야 함
            throw new IllegalArgumentException("유효하지 않은 주소이거나, 좌표를 찾을 수 없습니다.");
        }

        // 검색 결과 목록에서 첫 번째 주소 정보를 꺼낸다.
        NaverGeocodeDto.Address firstAddress = geocodeResult.addresses().get(0);
        // 네이버는 경도(x), 위도(y)를 문자열로 주기 때문에 숫자로 변환
        double lng = Double.parseDouble(firstAddress.x()); //경도
        double lat = Double.parseDouble(firstAddress.y()); //위도

        // 3. 네이버에게 받은 좌표로 RideRequest 객체 생성
        RideRequest newRequest = new RideRequest(
                requestDto.region(),
                requestDto.requestedTime(),
                lat,
                lng
        );
        rideRequestRepository.save(newRequest);
        System.out.println("좌표 변환 성공: " + requestDto.destinationAddess() + " -> lat: " + lat + ", lng : " + lng);
    }
}
