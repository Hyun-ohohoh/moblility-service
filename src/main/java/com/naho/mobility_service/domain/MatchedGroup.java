package com.naho.mobility_service.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA가 객체를 생성할 때 필요한 기본 생성자
@Entity
public class MatchedGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime finalDepartureTime; //확정된 최종 출발 시간

    private String virtualStopJson; // 가상 정류장 목록(JSON 형태의 문자열로 저장)

    private String rideRequestIds; //매칭된 요청들의 ID 목록

    // Builder 패턴: 객체를 생성할 때 실수를 줄이고 명확하게 값을 할당하기 위한 방법
    @Builder
    public MatchedGroup(LocalDateTime finalDepartureTime, String virtualStopJson, String rideRequestIds){
        this.finalDepartureTime = finalDepartureTime;
        this.virtualStopJson = virtualStopJson;
        this.rideRequestIds = rideRequestIds;
    }



}
