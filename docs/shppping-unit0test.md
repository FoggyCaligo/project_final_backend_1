# 쇼핑 최저가 단위테스트 산출물

작성일: 2026-05-06  
작성 대상 기능: 식재료 최저가 조회, 냉장고 전체 최저가 조회, 키워드 실시간 검색

---

## 1. 테스트 범위 및 파일 구성

| 테스트 파일 | 대상 클래스 | 테스트 유형 |
|---|---|---|
| `ShoppingControllerTest` | `ShoppingController` | Controller (WebMvcTest) |
| `ShoppingService3Test` | `ShoppingService3` | Service (Mockito) |

---

## 2. ShoppingControllerTest

**테스트 설정**
- `@WebMvcTest(ShoppingController.class)` — Security 자동설정 제외
- MockBean: `ShoppingService3`

### 2-1. 식재료 최저가 조회 `GET /api/v1/shopping/ingredients/{ingredientId}/prices`

| ID | 테스트명 | 입력 | 기대 결과 |
|---|---|---|---|
| SC-IP-01 | X-User-Id 헤더와 유효한 ingredientId 요청 시 성공 | `X-User-Id: 10`, `ingredientId: 1` | `success: true`, `data.ingredientName`, `data.lowestPrice` 포함 |
| SC-IP-02 | X-User-Id 헤더 없이 요청 시 인증 에러 | 헤더 없음 | HTTP 401, `success: false`, `code: UNAUTHORIZED` |

### 2-2. 냉장고 전체 최저가 조회 `GET /api/v1/shopping/fridge/prices`

| ID | 테스트명 | 입력 | 기대 결과 |
|---|---|---|---|
| SC-FP-01 | X-User-Id 헤더로 냉장고 전체 최저가 반환 | `X-User-Id: 10` | `success: true`, `data` 배열 길이 2 |
| SC-FP-02 | X-User-Id 헤더 없이 요청 시 인증 에러 | 헤더 없음 | HTTP 401, `code: UNAUTHORIZED` |
| SC-FP-03 | 냉장고가 비어있을 때 빈 배열 반환 | `X-User-Id: 10`, 서비스 빈 리스트 반환 | `success: true`, `data` 빈 배열 |

### 2-3. 키워드 실시간 검색 `GET /api/v1/shopping/search?keyword=...`

| ID | 테스트명 | 입력 | 기대 결과 |
|---|---|---|---|
| SC-SK-01 | 유효한 키워드로 실시간 검색 성공 | `keyword=계란` | `success: true`, `data.ingredientName: 계란` |
| SC-SK-02 | 공백만 있는 키워드 요청 시 입력값 에러 | `keyword=   ` | HTTP 400, `code: INVALID_INPUT` |

---

## 3. ShoppingService3Test

**테스트 설정**
- `@ExtendWith(MockitoExtension.class)`
- Mock: `RedisTemplate`, `ValueOperations`, `ObjectMapper`, `ShoppingItemRepository`, `IngredientMasterRepository`, `NaverShoppingClient`, `ElevenStShoppingClient`, `EntityManager`

### 캐시 전략 개요

```
요청
 │
 ├─ Redis 캐시 HIT → 즉시 반환
 │
 ├─ Redis MISS → DB 캐시(shopping_item_mcp) 확인
 │               ├─ DB HIT → 반환 + Redis 저장
 │               └─ DB MISS → 외부 API (네이버 + 11번가) 병렬 호출
 │                            → DB 저장 + Redis 저장
 └─ 외부 API 모두 실패 → ALL_SHOPPING_API_FAILED 예외
```

### 3-1. 식재료 최저가 조회 `getIngredientPrices(Long ingredientMasterId)`

| ID | 테스트명 | 시나리오 | 검증 항목 |
|---|---|---|---|
| SS-IP-01 | Redis 캐시 HIT 시 외부 API 미호출 | Redis에 캐시 존재 | 결과 반환, NaverClient·ElevenStClient 미호출 |
| SS-IP-02 | Redis MISS, DB 캐시 HIT 시 DB 결과 반환 | Redis miss, DB에 유효 캐시 | `lowestPrice: 2000`, Redis 저장 호출, 외부 API 미호출 |
| SS-IP-03 | Redis·DB 모두 MISS 시 외부 API 호출 후 저장 | 캐시 전무, 네이버 1800원·11번가 2200원 | `lowestPrice: 1800`, `saveAll` 호출, Redis 저장 호출 |
| SS-IP-04 | 존재하지 않는 ingredientMasterId | `findById` empty | `BusinessException` 발생, 메시지: "식재료 마스터 정보를 찾을 수 없습니다" |
| SS-IP-05 | 모든 외부 API 빈 결과 | 네이버·11번가 모두 빈 리스트 | `BusinessException` 발생, 메시지: "모든 쇼핑 API 호출에 실패했습니다" |

### 3-2. 냉장고 전체 최저가 조회 `getFridgePrices(Long userId)`

| ID | 테스트명 | 시나리오 | 검증 항목 |
|---|---|---|---|
| SS-FP-01 | Redis 냉장고 캐시 HIT 시 DB·외부 API 미호출 | `shopping:fridge:{userId}` 캐시 존재 | 결과 크기 2, EntityManager 미호출 |
| SS-FP-02 | 냉장고에 재료가 없을 때 빈 리스트 반환 | JPQL 결과 빈 리스트 | 빈 `List` 반환 |
| SS-FP-03 | 냉장고 재료 있을 때 재료별 가격 조회 후 30분 Redis 캐시 | 재료 1개, DB 캐시 HIT | 결과 크기 1, `set(..., 30, MINUTES)` 호출 |

### 3-3. 키워드 실시간 검색 `searchByKeyword(String keyword)`

| ID | 테스트명 | 시나리오 | 검증 항목 |
|---|---|---|---|
| SS-SK-01 | 키워드 Redis 캐시 HIT 시 외부 API 미호출 | `shopping:keyword:{keyword}` 캐시 존재 | 결과 반환, NaverClient 미호출 |
| SS-SK-02 | 네이버·11번가 API 성공 시 최저가 반환 및 캐시 | 네이버 1800원, 11번가 2200원 | `lowestPrice: 1800`, `items` 크기 2, Redis 저장 |
| SS-SK-03 | 양쪽 API 모두 빈 결과 시 빈 items 응답 (예외 없음) | 모두 빈 리스트 | `items` 빈 배열, `lowestPrice: 0`, 예외 미발생 |

---

## 4. 주요 설계 결정

| 항목 | 결정 사항 |
|---|---|
| Redis 장애 처리 | `try-catch`로 감싸 Redis 장애 시 DB/외부 API로 자동 fallback |
| 외부 API 병렬 호출 | `CompletableFuture.supplyAsync`로 네이버·11번가 동시 호출 |
| 11번가 결과 선별 | `getIngredientPrices`에서 11번가 결과 중 최저가 1개만 사용 |
| 키워드 검색 실패 정책 | 모두 실패 시 예외 대신 빈 `items` 응답 반환 (ingredient_master 검색과 다름) |
| 만료 캐시 정리 | `cleanupExpiredAsync` — 외부 API 호출 후 비동기 background 처리 |

---

## 5. 테스트 커버리지 범위

| 기능 | 정상 흐름 | 예외 흐름 |
|---|---|---|
| Redis 캐시 HIT | ✅ SS-IP-01, SS-FP-01, SS-SK-01 | - |
| DB 캐시 HIT | ✅ SS-IP-02 | - |
| 외부 API 호출 | ✅ SS-IP-03, SS-SK-02 | ✅ SS-IP-05 |
| ingredientMaster 미조회 | - | ✅ SS-IP-04 |
| 냉장고 비어있음 | ✅ SS-FP-02, SC-FP-03 | - |
| 키워드 빈 결과 | ✅ SS-SK-03 | - |
| 인증 없는 요청 | - | ✅ SC-IP-02, SC-FP-02 |
| 빈 키워드 입력 | - | ✅ SC-SK-02 |
