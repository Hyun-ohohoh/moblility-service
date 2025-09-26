package com.naho.mobility_service.service;

import com.naho.mobility_service.domain.*;
import com.naho.mobility_service.repository.MatchedGroupRepository;
import com.naho.mobility_service.repository.RideRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final MatchedGroupRepository matchedGroupRepository;


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
        // 1. 그룹의 최종 출발 시간 계산
        LocalDateTime finalDepartureTime = calculateFinalDepartureTime(group);

        // 2. 클러스터링으로 가상 정류장 생성
        List<VirtualStop> virtualStops = createVirtualStops(group);

//        System.out.println(group.get(0).getRegion() + " 행 매칭 성공! 최종 출발 시간: " + finalDepartureTime);
//        System.out.println("생성된 가상 정류장: " + virtualStops);

        // 3. 매칭 결과를 데이터베이스에 저장
        // 3-1. 가상 정류장 객체 리스트를 DB에 저장하기 위해 문자열로 변환합니다.
        String virtualStopJson = virtualStops.toString();

        // 3-2. 이 그룹에 속한 RideRequest 객체들의 ID만 추출하여 "1,5,12" 형태의 문자열로 만듭니다.
        String rideRequestIds = group.stream()
                .map(req -> req.getId().toString()) // 각 요청(req)에서 ID를 가져와 문자열로 바꾸고,
                .collect(Collectors.joining(",")); // 쉼표(,)로 이어붙인다.

        // 3-3. 위에서 계산한 정보들을 바탕으로 DB에 저장할 MatchedGroup 객체를 생성
        MatchedGroup newGroup = MatchedGroup.builder()
                .finalDepartureTime(finalDepartureTime)
                .virtualStopJson(virtualStopJson)
                .rideRequestIds(rideRequestIds)
                .build();

        // 3-4. MatchedGroupRepository를 통해 완성된 그룹 정보를 DB에 저장(INSERT)한다.
        matchedGroupRepository.save(newGroup);

        // 4. 기존 RideRequest들의 상태를 'MATCHED'로 변경
        // 이 그룹에 속해있던 모든 RideRequest 객체의 상태를 PENDING -> MATCHED로 변경합니다.
        // 이 메소드는 @Transactional 안에서 실행되므로, 상태 변경 후 save를 호출하지 않아도
        // JPA가 "객체가 변경되었네?"라고 감지하여 자동으로 DB에 UPDATE 쿼리를 날려줍니다.
        group.forEach(req -> req.updateStatus(RequestStatus.MATCHED));

        // 콘솔에 로그 출력
        System.out.println("매칭 성공! 그룹 ID: " + newGroup.getId() + ", 최종 출발 시간: " + finalDepartureTime);

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
        DBSCANClusterer<DoublePoint> clusterer = new DBSCANClusterer<>(0.005, 2);

        // 3. 클러스터링을 실행하고 결과를 받음
        List<Cluster<DoublePoint>> clusterResults = clusterer.cluster(points);

        List<VirtualStop> virtualStops = new ArrayList<>();

        // 4. 결과에서 각 클러스터의 중심점(가상 정류장) 좌표를 추출
        for (Cluster<DoublePoint> cluster : clusterResults) {
            double sumLat = 0;
            double sumLng = 0;
            for (DoublePoint point : cluster.getPoints()){
                sumLat += point.getPoint()[0];
                sumLng += point.getPoint()[1];
            }
            double centerLat = sumLat / cluster.getPoints().size();
            double centerLng = sumLng / cluster.getPoints().size();
            virtualStops.add(new VirtualStop(centerLat, centerLng));
        }

        return virtualStops;
    }
}
