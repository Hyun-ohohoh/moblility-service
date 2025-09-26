package com.naho.mobility_service.repository;

import com.naho.mobility_service.domain.RequestStatus;
import com.naho.mobility_service.domain.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    //Spring Data JPA가 메서드 이름을 보고 자동으로 쿼리를 만들어 줌
    // "Status가 PENDING이고, requestedTime이 두 시간 사이인 모든 RideRequest를 찾아줘"
    List<RideRequest> findAllByStatusAndRequestedTimeBetween(
            RequestStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}
