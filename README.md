# java-convenience-store-precourse

## 프로젝트 개요

편의점 결제 시스템으로, 구매자의 할인 혜택과 재고 상황을 고려하여 최종 결제 금액을 계산하고 안내하는 프로그램입니다.

**기본 기능**
- 상품의 가격과 수량 기반 결제 금액 계산
- 프로모션 및 멤버십 할인 정책 적용
- 구매 내역 및 금액 정보 영수증 출력
- 추가 구매 또는 종료 선택 기능

**재고 관리**
- 상품별 재고 수량 관리
- 구매 시 실시간 재고 차감
- 정확한 재고 정보 제공

**프로모션 할인**
- 프로모션 기간 내 할인 적용
- N+1 형태의 프로모션 운영 (1+1, 2+1)
- 프로모션 재고 우선 차감 정책
- 프로모션 혜택 안내 기능

**멤버십 할인**
- 프로모션 미적용 금액의 30% 할인
- 최대 할인 한도 8,000원
- 프로모션 적용 후 잔액에 대해 할인 적용

**영수증 출력**
- 구매 상품 내역 (상품명, 수량, 가격)
- 증정 상품 내역
- 금액 정보
  - 총구매액
  - 행사할인
  - 멤버십할인
  - 최종 결제 금액

**예외 처리**
- 잘못된 입력값에 대한 IllegalArgumentException 처리
- "[ERROR]" 메시지 출력 후 재입력 요청
- 명확한 Exception 타입 사용


# 구현 흐름

1. 상품 구매 입력 받기
    - [x] 구매할 상품과 수량 입력
        - [x] 상품명이 존재하는지 확인
        - [x] 수량이 재고보다 많은지 확인
        - [x] 예외 처리
            - [x] 상품명이 존재하지 않으면 예외 발생
            - [x] 수량이 0 이하면 예외 발생
            - [x] 수량이 재고보다 많으면 예외 발생
    - [x] 멤버십 카드 여부 입력
        - [x] Y/N으로만 입력 가능
        - [x] 예외 처리
            - [x] Y/N 이외의 값 입력시 예외 발생

2. 결제 프로세스
    - [x] 프로모션 적용 확인
        - [x] 상품별 프로모션 여부 확인 (1+1, 2+1)
        - [x] 프로모션 재고 확인
        - [x] 프로모션 안내 메시지 출력
    - [x] 멤버십 할인 계산
        - [x] 프로모션 적용 후 금액의 30% 할인
        - [x] 최대 8,000원 한도 확인
    - [x] 재고 차감
        - [x] 프로모션 재고 우선 차감
        - [x] 일반 재고 차감

3. 영수증 출력
    - [x] 구매 상품 내역 출력
    - [x] 증정 상품 내역 출력
    - [x] 금액 정보 출력
        - [x] 총구매액 계산 및 출력
        - [x] 행사 할인 금액 계산 및 출력
        - [x] 멤버십 할인 금액 계산 및 출력
        - [x] 최종 결제 금액 계산 및 출력

# 세부 구현 목록

- **도메인 계층**
  - [x] Product 클래스 구현
    - [x] 상품 정보 (이름, 가격, 수량) 저장
    - [x] 프로모션 정보 저장
    - [x] 재고 확인 기능
  - [x] Promotion 클래스 구현
    - [x] 프로모션 정보 (이름, 구매수량, 증정수량, 기간) 저장
    - [x] 프로모션 유효성 검증
    - [x] 증정 수량 계산
      - [x] 최종 결제 금액 계산
  - [x] Receipt 클래스 구현 
    - [x] 구매 상품 내역 관리 (상품명, 수량, 금액)
    - [x] 증정 상품 내역 관리 (상품명, 수량)
    - [x] 금액 정보 관리
        - [x] 총구매액 계산
        - [x] 행사할인 금액 계산
        - [x] 멤버십할인 금액 계산
        - [x] 최종 결제 금액 계산
  - [x] ReceiptItem 클래스 구현
    - [x] 상품명, 수량, 금액 정보 관리 

- **리포지토리 계층**
  - [x] ProductRepository 클래스 구현
    - [x] 상품 정보 로드 및 관리
    - [x] 상품 조회 기능
  - [x] PromotionRepository 클래스 구현
    - [x] 프로모션 정보 로드 및 관리
    - [x] 프로모션 조회 기능

- **서비스 계층**
  - [x] StoreService 클래스 구현
    - [x] 상품 구매 로직
    - [x] 할인 적용 로직
    - [x] 재고 관리 로직

- **프레젠테이션 계층**
  - [x] StoreController 클래스 구현
    - [x] 사용자 입력 처리
      - [x] InputView를 통한 입력 받기
      - [x] 입력값 유효성 검증
      - [x] 예외 발생시 재입력 요청
    - [x] 비즈니스 로직 처리
      - [x] StoreService 호출
      - [x] 결과 데이터 가공
    - [x] 결과 출력 관리
      - [x] OutputView를 통한 결과 출력
      - [x] 에러 메시지 출력 관리
    - [x] 프로그램 흐름 제어
      - [x] 구매 프로세스 진행
      - [x] 추가 구매 여부에 따른 분기 처리
      - [x] 프로그램 종료 처리

- **입출력 계층**
  - [x] InputConsole 클래스 구현
    - [x] 상품 구매 입력
      - [x] "구매할 상품명과 수량을 입력해 주세요." 출력
      - [x] 상품명과 수량을 "상품명-수량" 형식으로 입력 받기
      - [x] 여러 상품 입력시 쉼표(,)로 구분
      - [x] 입력 형식 검증
    - [x] 멤버십 카드 입력
      - [x] "멤버십 카드를 가지고 계신가요? (Y/N)" 출력
      - [x] Y 또는 N 입력 받기
    - [x] 추가 구매 여부 입력
      - [x] "추가 구매하시겠습니까? (Y/N)" 출력
      - [x] Y 또는 N 입력 받기

  - [x] OutputConsole 클래스 구현
    - [x] 시작 메시지 출력
      - [x] "안녕하세요! 편의점에 오신 것을 환영합니다." 출력
    - [x] 프로모션 안내 메시지 출력
      - [x] 현재 진행 중인 프로모션 목록 출력
      - [x] 프로모션 적용 가능한 상품 안내
    - [x] 영수증 출력
      - [x] 구매 상품 내역 출력 (표 형식)
        - [x] 상품명, 수량, 가격 정렬
      - [x] 증정 상품 내역 출력
      - [x] 구분선 출력
      - [x] 금액 정보 출력
        - [x] 총구매액
        - [x] 행사할인 금액
        - [x] 멤버십할인 금액
        - [x] 최종 결제 금액
    - [x] 에러 메시지 출력
      - [x] "[ERROR]" 접두어 포함
      - [x] 구체적인 에러 원인 명시

- **애플리케이션 실행**
  - [x] Application 클래스에 main 메서드 구현
  - [x] StoreConsole 생성 및 실행

- **유틸리티**
  - [x] Validator 클래스 구현
    - [x] 입력값 형식 검증
    - [x] 상품명 유효성 검증
    - [x] 수량 유효성 검증
    - [x] 멤버십 입력 검증
  - [x] ResourceLoader 클래스 구현
    - [x] MD 파일 읽기
    - [x] 데이터 파싱
  - [x] Membership 클래스 구현
    - [x] 할인율 관리
    - [x] 최대 할인 한도 관리

# 추가 체크 목록
- [x] 더 세부적인 입력값 체크
- [x] 프로모션 기간 체크