package com.naho.mobility_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter // 각 필드의 Getter 메서드를 자동으로 만들어 줌(Lombok)
@NoArgsConstructor //기본 생성자를 자동으로 만들어 줌(Lombok)
@Entity // 이 클래스가 데이터베이스 테이블임을 나타냄(JPA)
public class RideRequest {

    @Id // 각 필드가 테이블의 PK임을 타나탬
    @GeneratedValue(strategy = GenerationType.IDENTITY) //DB가 ID를 자동으로 생성하고 관리하도록 함
    private Long id;

    private long userId; //요청한 사용자 ID

    @Enumerated(EnumType.STRING) //Enum 타입을 문자열 형태로 DB에 저장
    private RequestStatus status; //요청 상태

    private String region; //목적지 대권역

    private LocalDateTime requestedTime; //사용자가 희망한 출발 시간

    private double destLat; //목적지 위도

    private double destLng; //목적지 경도

    //상태를 변경하는 메서드
    public void updateStatus(RequestStatus status){
        this.status = status;
    }



}
