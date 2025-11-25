# 🌙 Seoul Night DRT (서울 심야 수요응답형 교통 서비스)

> **심야 시간대 대중교통 사각지대 해소를 위한 동적 가상 정류장(Virtual Stop) 생성 및 배차 백엔드 서버**

![Generic badge](https://img.shields.io/badge/Java-17-blue.svg) ![Generic badge](https://img.shields.io/badge/SpringBoot-3.x-green.svg) ![Generic badge](https://img.shields.io/badge/MySQL-8.0-orange.svg) ![Generic badge](https://img.shields.io/badge/Status-Prototype-yellow.svg)

## 📖 프로젝트 소개 (Introduction)
심야 시간(00:00 ~ 04:00)의 택시 승차난과 대중교통 부족 문제를 해결하기 위해 기획된 **수요응답형 교통(DRT)** 서비스입니다. 
고정된 정류장을 사용하는 기존 버스와 달리, **실시간 호출 위치를 기반으로 최적의 '가상 정류장(Virtual Stop)'을 동적으로 생성**하여 운행 효율을 높이는 백엔드 로직을 구현했습니다.

## 🛠 핵심 기능 (Key Features)

### 1. 주소 기반 좌표 변환 (Geocoding)
- **Naver Maps API 연동:** 사용자가 입력한 도로명 주소를 `NaverApiService`를 통해 위도/경도(Latitude/Longitude) 좌표로 정밀하게 변환합니다.
- **RestTemplate 활용:** 외부 API와의 통신을 위한 HTTP Client 구현 및 응답 DTO 파싱 처리를 완료했습니다.

### 2. 가상 정류장 생성 알고리즘 (Clustering)
- **DBSCAN 기반 클러스터링:** 사용자들의 요청 위치를 분석하여 밀도가 높은 구역을 그룹핑합니다.
- **동적 위치 선정:** 1. 특정 반경(약 300~500m) 내에 호출이 모여있는지 분석.
    2. 그룹화된 승객들의 좌표 평균값(Centroid)을 계산하여 **새로운 가상 정류장** 생성.
    3. 그룹에 속하지 못한 외곽 지역 승객(Noise)은 개별 정류장으로 처리.

### 3. 실시간 통합 매칭 시스템
- **Time Window Scheduling:** `@Scheduled`를 사용하여 매 시간 정각마다 기준 시간 ±30분 내의 요청을 모아 일괄 처리합니다.
- **매칭 로직:**
    - 권역(Region)별 요청 그룹핑 (예: 동탄, 일산 등).
    - 최소 탑승 인원 미달 시 자동 취소(`CANCELED_NO_CAPACITY`) 처리.
    - 매칭 성공 시 `MatchedGroup` 엔티티 생성 및 JSON 형태로 정류장 정보 저장.

## 📚 기술 스택 (Tech Stack)

| 구분 | 기술 | 상세 내용 |
| --- | --- | --- |
| **Language** | Java 17 | Record 등 최신 문법 활용 |
| **Framework** | Spring Boot 3.x | Web, Validation, Scheduler |
| **Database** | MySQL / H2 | Spring Data JPA, Spatial Data 처리 |
| **External API** | Naver Cloud Platform | Geocoding (Maps) |
| **Algorithm** | DBSCAN | 밀도 기반 클러스터링 알고리즘 적용 |

## 🏗 시스템 흐름도 (System Flow)

1. **Ride Request:** 클라이언트가 목적지 주소와 희망 시간을 입력하여 요청 (`POST /api/requests`)
2. **Coordinate Conversion:** 백엔드에서 Naver API를 호출하여 주소를 좌표(x, y)로 변환 후 DB 저장
3. **Batch Processing:** 스케줄러가 `PENDING` 상태의 요청들을 주기적으로 조회
4. **Clustering & Matching:**
    - 지역별(Region)로 요청 분류
    - DBSCAN 알고리즘으로 근거리 승객 그룹핑
    - 그룹의 무게중심을 계산하여 `Virtual Stop` 생성
5. **Persistence:** 매칭 결과(`MatchedGroup`) 저장 및 승객 상태 업데이트(`MATCHED`)

## 🚧 제한 사항 및 참고 (Limitations & Note)

1. **External API Dependencies**
   - 주소 변환(Geocoding) 기능은 Naver Maps API에 의존합니다.
   - 현재 리포지토리에는 **API Key가 포함되어 있지 않으므로**, 클론 후 실행 시 별도의 API Key 발급 및 설정이 필요합니다.
   
2. **Prototype Scope**
   - 본 프로젝트는 **백엔드 핵심 로직(매칭 알고리즘, 좌표 변환)** 검증을 위한 프로토타입입니다.
   - 실제 도로망(Routing) 데이터 대신 직선 거리를 기반으로 매칭됩니다.
