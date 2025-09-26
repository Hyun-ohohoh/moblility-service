package com.naho.mobility_service.domain;

public enum RequestStatus {
    PENDING,   //매칭 대기 중
    MATCHED,   //매칭 성공
    CANCELED_NO_CAPACITY //인원 미달로 취소
}
