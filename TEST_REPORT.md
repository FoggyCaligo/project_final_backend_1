# 테스트 산출물 보고서

> 작성일: 2026-05-06
> 작성자: 쇼핑/유저 파트 담당
> 대상 모듈: `user`, `shopping`

---

## 1. 테스트 전략

### 1-1. 계층 구분

| 구분 | 사용 기술 | 특징 |
|------|-----------|------|
| **단위 테스트** | `@ExtendWith(MockitoExtension.class)` / `@WebMvcTest` | 단일 클래스 격리, Mock 의존성 |
| **통합 테스트** | `@SpringBootTest` + `@AutoConfigureMockMvc` | 전체 Spring 컨텍스트, H2 인메모리 DB |

### 1-2. 공통 Mock 전략

| 외부 의존성 | Mock 방법 | 이유 |
|-------------|-----------|------|
| Redis (`RedisTemplate`) | `@MockBean` | 테스트 환경에 Redis 서버 없음 |
| 네이버 쇼핑 API (`NaverShoppingClient`) | `@MockBean` | 실제 API 키 없이 동작 검증 |
| 11번가 API (`ElevenStShoppingClient`) | `@MockBean` | 실제 API 키 없이 동작 검증 |
| 이메일 서비스 (`EmailService`) | `@MockBean` | 실제 메일 발송 방지 |

### 1-3. DB 전략

- **통합 테스트**: H2 인메모리 DB (`application-test.yml`, `MODE=PostgreSQL`)
- **단위 테스트**: DB 접근 없음 (Repository도 Mock)
- 각 테스트는 `@Transactional`로 데이터 자동 롤백

### 1-4. 인증 전략

| 테스트 유형 | 인증 방법 |
|-------------|-----------|
| 단위 (WebMvcTest) | Security 필터 제외, 컨트롤러 내부 `requireUserId()` 로직으로 검증 |
| 통합 (SpringBootTest) | `@WithMockUser(username = "loginId")` → `SecurityContextHolder` 주입 |

---

## 2. 테스트 파일 목록

```
server/src/test/java/com/today/fridge/
├── user/
│   ├── service/
│   │   └── UserServiceTest.java             (기존 단위 테스트)
│   ├── controller/
│   │   └── UserControllerTest.java          (기존 단위 테스트)
│   └── integration/
│       └── UserApiIntegrationTest.java      ★ 신규 통합 테스트
│
└── shopping/
    ├── external/
    │   ├── naver/
    │   │   └── NaverShoppingClientTest.java    ★ 신규 단위 테스트 (JSON 파싱·필터링 검증)
    │   └── elevenst/
    │       └── ElevenStShoppingClientTest.java ★ 신규 단위 테스트 (XML 파싱·필터링 검증)
    ├── service/
    │   └── ShoppingService3Test.java        ★ 신규 단위 테스트
    ├── controller/
    │   └── ShoppingControllerTest.java      ★ 신규 단위 테스트
    └── integration/
        └── ShoppingApiIntegrationTest.java  ★ 신규 통합 테스트

frontend/src/__tests__/api/
└── shoppingApi.test.js                      ★ 신규 프론트엔드 단위 테스트
```

---

## 3. User 테스트 케이스

### 3-1. UserServiceTest (기존 단위 테스트) — 18개

| # | 메서드 | 시나리오 | 기댓값 |
|---|--------|----------|--------|
| 1 | `signup` | 정상 요청 | 예외 없음, `save()` 호출 |
| 2 | `signup` | loginId 중복 | `DUPLICATE_LOGIN_ID` |
| 3 | `signup` | email 중복 | `DUPLICATE_EMAIL` |
| 4 | `signup` | nickname 중복 | `DUPLICATE_NICKNAME` |
| 5 | `signup` | 비밀번호 8자 미만 | `INVALID_INPUT_VALUE` |
| 6 | `signup` | 비밀번호 특수문자 없음 | `INVALID_INPUT_VALUE` |
| 7 | `signup` | 비밀번호 숫자 없음 | `INVALID_INPUT_VALUE` |
| 8 | `signup` | 비밀번호 영문자 없음 | `INVALID_INPUT_VALUE` |
| 9 | `normalizeEmail` | 앞뒤 공백·대문자 | 소문자·트림 반환 |
| 10 | `normalizeEmail` | null 입력 | null 반환 |
| 11 | `findLoginIdByEmail` | 등록 이메일 | loginId 반환 |
| 12 | `findLoginIdByEmail` | 미등록 이메일 | `USER_NOT_FOUND` |
| 13 | `findLoginIdByEmail` | 대소문자 무관 | 정규화 후 조회 |
| 14 | `isLoginIdAvailable` | 미사용 아이디 | `true` |
| 15 | `isLoginIdAvailable` | 사용 중 아이디 | `false` |
| 16 | `updateProfile` | 닉네임 변경 | 변경된 프로필 반환 |
| 17 | `updateProfile` | 닉네임 중복 | `DUPLICATE_NICKNAME` |
| 18 | `updateProfile` | 자기 닉네임 유지 | 예외 없음 |

### 3-2. UserControllerTest (기존 단위 테스트) — 7개

| # | 엔드포인트 | 시나리오 | 기댓값 |
|---|-----------|----------|--------|
| 1 | `GET /find-loginid` | 등록 이메일 | 200, loginId 반환 |
| 2 | `GET /find-loginid` | 미등록 이메일 | `USER_NOT_FOUND` |
| 3 | `GET /me/profile` | 인증 성공 | 200, 프로필 반환 |
| 4 | `PATCH /me/profile` | 닉네임 변경 | 200, 변경된 프로필 |
| 5 | `PATCH /me/profile` | 닉네임 중복 | `DUPLICATE_NICKNAME` |
| 6 | `PATCH /me/password` | 정상 변경 | 200, 성공 메시지 |
| 7 | `PATCH /me/password` | 현재 비밀번호 불일치 | `UNAUTHORIZED` |

### 3-3. UserApiIntegrationTest (신규 통합 테스트) — 11개

| # | 엔드포인트 | 시나리오 | 기댓값 |
|---|-----------|----------|--------|
| 1 | `GET /find-loginid` | 등록된 이메일 | 200, loginId 반환 |
| 2 | `GET /find-loginid` | 미등록 이메일 | `USER_NOT_FOUND` |
| 3 | `GET /find-loginid` | 이메일 형식 오류 | 400 |
| 4 | `GET /me/profile` | `@WithMockUser` 인증 | 200, DB의 실제 프로필 |
| 5 | `GET /me/profile` | 미인증 | `UNAUTHORIZED` |
| 6 | `PATCH /me/profile` | 닉네임 변경 | 200, DB 반영 |
| 7 | `PATCH /me/profile` | 타 유저 닉네임 중복 | `DUPLICATE_NICKNAME` |
| 8 | `PATCH /me/profile` | 자기 닉네임 유지 | 200, 정상 처리 |
| 9 | `PATCH /me/password` | 현재 비밀번호 일치 | 200, 성공 |
| 10 | `PATCH /me/password` | 현재 비밀번호 불일치 | `UNAUTHORIZED` |
| 11 | `PATCH /me/password` | 새 비밀번호 규칙 위반 | `INVALID_INPUT_VALUE` |

---

## 4. Shopping 테스트 케이스

### 4-1. ShoppingService3Test (신규 단위 테스트) — 6개

| # | 메서드 | 시나리오 | 기댓값 |
|---|--------|----------|--------|
| 1 | `searchByKeyword` | Redis 캐시 HIT | 캐시 반환, 외부 API 호출 없음 |
| 2 | `searchByKeyword` | Redis MISS | 네이버+11번가 호출, 최저가 반환 |
| 3 | `searchByKeyword` | 모든 API 실패 | 빈 응답 반환 (예외 없음) |
| 4 | `getIngredientPrices` | Redis HIT | 캐시 반환, DB/API 호출 없음 |
| 5 | `getIngredientPrices` | DB 캐시 HIT | DB 결과 반환, Redis 재저장 |
| 6 | `getIngredientPrices` | 완전 캐시 MISS | 외부 API 호출, DB 저장 |

### 4-2. NaverShoppingClientTest (신규 단위 테스트) — 6개

| # | 시나리오 | Given (JSON 응답) | 기댓값 |
|---|----------|-------------------|--------|
| 1 | 정상 응답 → 최저가 1건 반환 | 사과 15000원 | 결과 1건, price=15000 |
| 2 | 가격 < 500원 → 필터 | 사과 스티커 300원 | 결과 0건 |
| 3 | items 빈 배열 → 빈 리스트 | items: [] | 결과 0건 |
| 4 | 여러 상품 → 최저가(3500원)만 반환 | [5000원, 3500원, 7000원] | 결과 1건, price=3500 |
| 5 | RestClientException → 빈 리스트 | 네트워크 오류 | 결과 0건 (예외 전파 X) |
| 6 | API 키 미설정 → 빈 리스트 | clientId="" | 결과 0건 |

### 4-3. ElevenStShoppingClientTest (신규 단위 테스트) — 5개

| # | 시나리오 | Given (XML 상품명/가격) | 기댓값 |
|---|----------|------------------------|--------|
| 1 | 가격 < 1000원 → 필터 | "수박 자르개", 900원 | 결과 0건 |
| 2 | 비식품 키워드(커터) 포함 → 필터 | "수박 껍질 커터", 2500원 | 결과 0건 |
| 3 | 정상 식재료 → 통과 | "수박 5kg 당일배송", 15000원 | 결과 1건 |
| 4 | 식품+비식품 혼합 → 식품만 반환 | ["수박 5kg" 15000원, "수박 도구" 900원] | 결과 1건 |
| 5 | 키워드 미포함 상품 → 필터 | "참외 1통", 5000원, keyword="수박" | 결과 0건 |

### 4-4. ShoppingControllerTest (신규 단위 테스트) — 9개

| # | 엔드포인트 | 시나리오 | 기댓값 |
|---|-----------|----------|--------|
| 1 | `GET /search?keyword=계란` | 정상 키워드 | 200, 결과 반환 |
| 2 | `GET /search?keyword=   ` | 빈 keyword | 400 |
| 3 | `GET /search` | keyword 파라미터 없음 | 400 |
| 4 | `GET /ingredients/1/prices` | X-User-Id 없음 | `UNAUTHORIZED` |
| 5 | `GET /ingredients/1/prices` | X-User-Id 있음 | 200, 결과 반환 |
| 6 | `GET /fridge/prices` | X-User-Id 없음 | `UNAUTHORIZED` |
| 7 | `GET /fridge/prices` | X-User-Id 있음 | 200, 배열 반환 |
| 8 | `GET /recipes/1/missing-ingredients-prices` | X-User-Id 없음 | `UNAUTHORIZED` |
| 9 | `GET /recipes/1/missing-ingredients-prices` | MISSING 재료 있음 | 200, 1건(수박) |
| 10 | `GET /recipes/1/missing-ingredients-prices` | 모든 재료 OK | 200, 빈 리스트 `[]` |

### 4-5. ShoppingApiIntegrationTest (신규 통합 테스트) — 9개

| # | 엔드포인트 | 시나리오 | 기댓값 |
|---|-----------|----------|--------|
| 1 | `GET /search?keyword=계란` | 네이버+11번가 Mock | 200, lowestPrice=2700 |
| 2 | `GET /search` | keyword 없음 | 400 |
| 3 | `GET /search?keyword=계란` | 모든 API 빈 결과 | 200, items=[] |
| 4 | `GET /ingredients/{id}/prices` | X-User-Id 없음 | `UNAUTHORIZED` |
| 5 | `GET /ingredients/{id}/prices` | 유효한 ID + 인증 | 200, 결과 반환 |
| 6 | `GET /ingredients/99999/prices` | 존재하지 않는 ID | 에러 응답 |
| 7 | `GET /fridge/prices` | X-User-Id 없음 | `UNAUTHORIZED` |
| 8 | `GET /recipes/{id}/missing-ingredients-prices` | 부족 재료(수박 MISSING) | 200, 1건, ingredientName="수박" |
| 9 | `GET /recipes/{id}/missing-ingredients-prices` | X-User-Id 없음 | `UNAUTHORIZED` |

---

## 5. Frontend Shopping 테스트 케이스

### 5-1. shoppingApi.test.js (신규 단위 테스트) — 10개

| # | 함수 | 시나리오 | 기댓값 |
|---|------|----------|--------|
| 1 | `searchByKeyword` | 정상 키워드 | GET `/v1/shopping/search?keyword=계란` 호출 |
| 2 | `searchByKeyword` | 서버 에러 | reject Promise |
| 3 | `searchByKeyword` | 빈 문자열 | 빈 keyword 그대로 전달 |
| 4 | `searchByKeyword` | 한글 키워드 | 응답 데이터 정상 반환 |
| 5 | `getIngredientPrices` | id=1 | GET `/v1/shopping/ingredients/1/prices` 호출 |
| 6 | `getIngredientPrices` | id=42 | GET `/v1/shopping/ingredients/42/prices` 호출 |
| 7 | `getIngredientPrices` | 인증 에러 | reject Promise |
| 8 | `getFridgePrices` | 정상 | GET `/v1/shopping/fridge/prices` 호출 |
| 9 | `getFridgePrices` | 여러 결과 | 배열 반환 |
| 10 | `getFridgePrices` | 냉장고 비어있음 | 빈 배열 반환 |

---

## 6. 테스트 실행 방법

### 서버 (Gradle)
```bash
# 전체 테스트 실행
cd server
./gradlew test

# User 관련 테스트만 실행
./gradlew test --tests "com.today.fridge.user.*"

# Shopping 관련 테스트만 실행
./gradlew test --tests "com.today.fridge.shopping.*"

# 특정 클래스만 실행
./gradlew test --tests "com.today.fridge.shopping.integration.ShoppingApiIntegrationTest"
./gradlew test --tests "com.today.fridge.shopping.external.naver.NaverShoppingClientTest"
./gradlew test --tests "com.today.fridge.shopping.external.elevenst.ElevenStShoppingClientTest"
```

### 프론트엔드 (Jest)
```bash
cd frontend

# 전체 테스트
npm test

# shoppingApi 테스트만 실행
npm test -- --testPathPattern=shoppingApi

# 모든 api 테스트 실행
npm test -- --testPathPattern=__tests__/api
```

---

## 7. 버그 수정 내역

| # | 문제 | 원인 | 수정 내용 |
|---|------|------|-----------|
| 1 | Swagger UI 500 에러 | `springdoc 2.6.0`이 Spring Boot 3.5.x 미지원 | `build.gradle`: `2.6.0` → `2.7.0` |
| 2 | 네이버 API 키 미설정 로깅 | `@Value("${naver.shopping.client-id:}")` 경로 오류 | `app.naver.shopping.client-id`로 수정 |
| 3 | 11번가 API 키 미설정 로깅 | `@Value("${elevenst.openapi.key:}")` 경로 오류 | `app.elevenst.openapi.key`로 수정 |
| 4 | 11번가 비식품(도구류) 최저가 반환 | 가격 하한선 없음(`> 0`) + 이름 포함 여부만 체크 | `ElevenStShoppingClient`: 가격 `< 1000` 필터 + `NON_FOOD_WORDS` 비식품 키워드 필터 추가 |
