package com.naho.mobility_service.service;

import com.naho.mobility_service.domain.RequestStatus;
import com.naho.mobility_service.domain.RideRequest;
import com.naho.mobility_service.domain.VirtualStop;
import com.naho.mobility_service.repository.RideRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.naho.mobility_service.domain.VirtualStop;
import org.apache.commons.*;

@Service //이 클래스가 비즈니스 로직을 담당하는 서비스 계층임을 나타냄
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어 줌(Lombok)
public class MatchingService {

    //application.properties에 설정한 값을 자동으로 주입해 줍니다.
    @Value("${mobility-app.matching.minimum-passengers}")
    private int MINIMUM_PASSENGERS;

    //DB에 접근하기 위한 리포지토리를 주입받음
    private final RideRequestRepository rideRequestRepository;

    /**
     * 매 시간 정각에 실행되어 매칭 로직을 시작
     */
    @Transactional // 이 메서드의 모든 DB 작업을 하나의 트랜잭션으로 묶는다
    @Scheduled(cron = "0 0 * * * *") //매시간 0분 0초에 실행
    public void runConsolidatedMatching(){
        // 1. 매칭의 기준이 될 '중심 시간'을 설정 (예: 20시에 실행된다면, 23:00가 중심 시간)
        LocalDateTime centralTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(3);

        // 2. 중심 시간 기준 ±30분 범위(총 1시간)를 매칭 대상 창(Window)으로 설정
        LocalDateTime windowStart = centralTime.minusMinutes(30);
        LocalDateTime windowEnd = centralTime.plusMinutes(30);

        System.out.println(
                "[" + LocalDateTime.now() + "] 통합 매칭 시스템 실행: " + windowStart + " ~ " + windowEnd + "사이의 모든 요청을 처리합니다."
        );

        // 3. 해당 시간 범위 내의 모든 PENDING 요청을 DB에서 불러옴
        List<RideRequest> candidate = rideRequestRepository.findAllByStatusAndRequestedTimeBetween(
                RequestStatus.PENDING, windowStart, windowEnd);

        if(candidate.isEmpty()){
            System.out.println("처리할 예약 요청이 없습니다.");
            return;
        }

        // 4. 대권역(region)별로 그룸핑
        Map<String, List<RideRequest>> requestsByRegion = candidate.stream()
                .collect(Collectors.groupingBy(RideRequest::getRegion));

        // 5. 각 그룹별로 매칭 시도
        requestsByRegion.forEach((region, group) -> {
            System.out.println(region + "행 그룹에 " + group.size() + "명의 후보가 있습니다.");
            if(group.size() < MINIMUM_PASSENGERS){
                //최소 인원 미달 시, 그룹 내 모든 요청 취소 처리
                cancelRequests(group);
            } else {
                // 매칭 성공! 그룹의 최종 출발 시간을 계산하고 매칭 처리
                processConsolidatedMatch(group);
            }
        });
    }

    private void cancelRequests(List<RideRequest> group){
        System.out.println(group.get(0).getRegion() + " 행 운행은 최소 인원 미달로 취소됩니다.");

        //그룹 내 모든 요청에 대해 반복
        for (RideRequest request : group) {
            // 1. 상태를 CANCELED_NO_CAPACITY로 변경
            request.updateStatus(RequestStatus.CANCELED_NO_CAPACITY);

            // 2. 변경된 상태를 DB에 저장 (JPA가 변경을 감지하고 저장해 줌)
            //@Transactional 안에서는 save를 명시적으로 호출하지 않아도 변경이 감지되어 DB에 반영됨
            //rideRequestRepository.save(request); // 명시적으로 호출해도 괜찮음

        }
    }

    private void processConsolidatedMatch(List<RideRequest> group){

    }

    private LocalDateTime calculateFinalDepartureTime(List<RideRequest> group){
        // 1. 그룹에 속한 모든 요청의 희망 시간을 초(epoch second) 단위로 변환하여 평균을 구합니다.
        long averageEpochSecond = (long) group.stream()
                .mapToLong(req -> req.getRequestedTime().toEpochSecond(ZoneOffset.UTC))
                .average()
                .orElse(0);

        // 2. 계산된 평균 초를 다시 LocalDateTime 객체로 변환
        LocalDateTime averageTime = LocalDateTime.ofEpochSecond(averageEpochSecond, 0, ZoneOffset.UTC);

        // 3. 계산된 시간을 가장 가까운 10분 단위로 반올림하여 최종 출발 시간으로 정함
        int minute = averageTime.getMinute();
        int roundedMinute = (int) (Math.round(minute / 10.0) * 10) % 60; //60분이 넘어가면 0으로

        // 분이 60에 가까워져 반올림될 경우를 대비해 시간(hour)도 조정이 필요할 수 있음
        if(roundedMinute == 0 && minute > 30){
            return averageTime.plusHours(1).withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        } else {
            return averageTime.withMinute(roundedMinute).truncatedTo(ChronoUnit.MINUTES);
        }
    }

    private List<VirtualStop> createVirtualStops(List<RideRequest> group){
        // 1. 요청 목록에서 좌표 목록을 생성
        List<DoublePoint> points = group.stream()
                .map(req -> new DoublePoint(new double[]{req.getDestLat(), req.getDestLng()}))
                .toList();

        // 2. DBSCAN 클러스터러 객체 생성
        // epsilon : 0.005는 대략 500m 반경을 의미함 (좌표계에 따라 튜닝 필요)
        // minPts: 최소 2명 이상 모여야 하나의 클러스터(정류장)를 형성합니다.

        return List.of();
    }
}
