package com.naho.mobility_service.service;


import com.naho.mobility_service.domain.Region;
import com.naho.mobility_service.domain.RideRequest;
import com.naho.mobility_service.repository.RideRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final RideRequestRepository rideRequestRepository;

    @Override
    public void run(String... args) throws Exception {
        rideRequestRepository.deleteAll();

        //3시간 뒤인 시간을 기준으로 매칭될 후보 데이터 생성
        LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(3);

        // DONGTAN행 그룹
        // 1. 동탄 북부
        RideRequest dongtan1 = new RideRequest(Region.DONGTAN, baseTime.minusMinutes(20), 37.215, 127.075);
        RideRequest dongtan2 = new RideRequest(Region.DONGTAN, baseTime.plusMinutes(10), 37.217, 127.074);

        //그룹 2: 동탄 남부
        RideRequest dongtan3 = new RideRequest(Region.DONGTAN, baseTime.plusMinutes(15), 37.185, 127.105);
        RideRequest dongtan4 = new RideRequest(Region.DONGTAN, baseTime.plusMinutes(25), 37.184, 127.107);

        // 그룹 3: 동탄역 인근 (새로 추가)
        RideRequest dongtan5 = new RideRequest(Region.DONGTAN, baseTime, 37.201, 127.100);
        RideRequest dongtan6 = new RideRequest(Region.DONGTAN, baseTime.plusMinutes(5), 37.202, 127.099);

        // --- "ILSAN" 행 그룹 (매칭 실패 예상: 2명 < 최소 4명) ---
        RideRequest ilsan1 = new RideRequest(Region.ILSAN, baseTime.minusMinutes(5), 37.663, 126.764);
        RideRequest ilsan2 = new RideRequest(Region.ILSAN, baseTime.plusMinutes(5), 37.668, 126.770);

        rideRequestRepository.saveAll(List.of(
                dongtan1, dongtan2, dongtan3, dongtan4, dongtan5, dongtan6,
                ilsan1, ilsan2
        ));
        System.out.println("✅ 테스트 데이터가 성공적으로 로드되었습니다. (동탄 6명, 일산 2명)");
    }

}