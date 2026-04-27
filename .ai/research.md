# Research — 민예린 담당 영역 구현 참고

## 1. 현재 프로젝트 구조 요약

### 기술 스택 (실제 코드 기준)
- **백엔드**: Spring Boot, Java (패키지: `com.today.fridge`)
- **DB**: PostgreSQL (`jdbc:postgresql://localhost:5435/today_fridge`) — docs에는 MariaDB로 표기되어 있으나 실제 코드는 PostgreSQL
- **FastAPI 연동**: `RestTemplate` 사용 (`AppConfig.java`에 Bean 등록됨)
- **파일 업로드**: 최대 단일 5MB, 요청 전체 25MB

### 도메인 패키지 구성

```
src/main/java/com/today/fridge/
├── auth/          # 인증/세션 (controller, entity, repository, service 구현됨)
├── bookmark/      # 북마크
├── chatbot/       # 챗봇 추천
├── dashboard/     # 홈 대시보드
├── file/          # 파일 업로드 메타데이터
├── global/
│   ├── config/    # AppConfig(RestTemplate), SecurityConfig, WebMvcConfig
│   ├── controller/# FastApiTestController (헬스체크 스텁)
│   ├── exception/ # GlobalExceptionHandler
│   ├── external/fastapi/ # FastApiService (현재 health()만 구현)
│   ├── response/  # ApiResponse (현재 빈 클래스)
│   └── util/      # DateTimeUtils
├── ingredient/    # 식재료 엔티티 구현됨, Controller는 빈 스텁
├── post/          # 게시판
├── recipe/        # 레시피 엔티티 구현됨, Controller는 빈 스텁
└── user/          # 사용자/팔로우/알림
```

---

## 2. 민예린 담당 구현 시 참고사항

### 2-1. FastApiService 확장 방법

현재 [FastApiService.java](src/main/java/com/today/fridge/global/external/fastapi/FastApiService.java)는 `health()`만 존재한다. 쇼핑/GraphRAG/대체 재료 추천 메서드를 이 클래스에 추가하거나, 기능별로 `ShoppingApiService`, `GraphRagService` 등 별도 클래스로 분리하여 추가해야 한다.

**application.yml FastAPI 연동 설정값** ([application.yml](src/main/resources/application.yml)):
```yaml
app:
  fastapi:
    base-url: http://localhost:8000   # FastAPI 서버 주소
    service-key: change-me             # X-Internal-Token 헤더값 (운영 전 변경 필수)
    caller-service: spring-backend     # X-Internal-Service 헤더값
```

내부 API 호출 시 의무 헤더 3종:
```
X-Internal-Service: spring-backend    (caller-service 값)
X-Internal-Token:   change-me         (service-key 값)
X-Request-Id:       {UUID}            (추적용, 매 요청 생성)
```

**현재 RestTemplate은 타임아웃 설정 없음** — 외부 쇼핑 MCP 호출처럼 응답 지연이 발생할 수 있는 곳에서는 `AppConfig.java`에서 `RestTemplate`에 `HttpComponentsClientHttpRequestFactory`로 connect/read timeout을 설정해야 한다.

---

### 2-2. 미구현 엔티티 (새로 생성 필요)

민예린 담당 DB 테이블 중 현재 엔티티 클래스가 없는 것들:

| 테이블 | 생성 위치 (권장) | 비고 |
|--------|----------------|------|
| `shopping_item_mcp` | `ingredient/entity/` 또는 `shopping/entity/` | `ingredient_master_id` FK |
| `ingredient_relation_graph` | `ingredient/entity/` 또는 `shopping/entity/` | self-join 구조 (`base_ingredient_id`, `target_ingredient_id`) |
| `vision_recognition_request` | `ingredient/entity/` | `file_id` → `file_asset` FK, `analysis_result` JSONB |

**기존 연관 엔티티 참고**:
- [IngredientMaster.java](src/main/java/com/today/fridge/ingredient/entity/IngredientMaster.java) — `ingredient_master_id` PK, `normalized_name` (unique), `alias_text`
- [UserIngredient.java](src/main/java/com/today/fridge/ingredient/entity/UserIngredient.java) — `ingredient_master_id` FK, `expires_at`, `freshness_status`
- [RecipeIngredient.java](src/main/java/com/today/fridge/recipe/entity/RecipeIngredient.java) — `ingredient_master_id` FK, `is_optional`, `amount_text`

---

### 2-3. 쇼핑 컨트롤러/서비스 신규 생성

현재 `shopping` 도메인 패키지가 없다. 아래 구조로 생성 권장:

```
shopping/
├── controller/  ShoppingController.java
├── service/     ShoppingService.java
├── entity/      ShoppingItemMcp.java, IngredientRelationGraph.java
└── repository/  ShoppingItemMcpRepository.java, IngredientRelationGraphRepository.java
```

담당 공개 API 3종 (ShoppingController에 구현):
- `GET /api/v1/shopping/lowest-price`
- `GET /api/v1/shopping/recipes/{recipeId}/missing-items`
- `GET /api/v1/shopping/alternatives`

비동기 이미지 인식 상태 조회는 `ingredient` 도메인의 `UserIngredientController`에 추가:
- `GET /api/v1/fridge/ingredients/recognize-image/status/{requestId}`

---

### 2-4. ApiResponse 공통 응답 형식

[ApiResponse.java](src/main/java/com/today/fridge/global/response/ApiResponse.java)가 현재 **빈 클래스**다. docs의 공통 응답 규격에 맞게 구현이 필요하며, 모든 응답에 이 클래스를 사용해야 한다. 내부 API 응답 예시 형식:

```json
{
  "success": true,
  "code": "OK",
  "message": "...",
  "data": { ... },
  "requestId": "req_xxx"
}
```

---

### 2-5. 보안 설정 현황

[SecurityConfig.java](src/main/java/com/today/fridge/global/config/SecurityConfig.java)는 현재 **모든 요청을 `permitAll()`** 처리 중이다 — JWT 인증 필터가 미구현 상태. 쇼핑 API 3종은 모두 인증 필요 API이므로, 인증 필터 구현 후 해당 경로에 인증 조건을 추가해야 한다. 별도 `global/security/SecurityConfig.java`도 존재하니 중복 주의.

---

### 2-6. 부족 재료 계산 로직

쇼핑 링크/최저가 조회의 전제인 "부족 재료 계산"은 ERD 설계 기준에 따라 **애플리케이션 계층에서 동적으로** 처리:

```
recipe_ingredient (레시피 필요 재료)
    └─ ingredient_master_id
         ↕ 비교
user_ingredient (사용자 보유 재료)
    └─ ingredient_master_id
```

두 테이블을 `ingredient_master_id` 기준으로 비교하여 사용자가 보유하지 않은 재료 목록을 추출 → 해당 재료명을 FastAPI 내부 API에 전달하는 흐름.

---

### 2-7. vision_recognition_request 비동기 흐름

```
POST /api/v1/fridge/ingredients/recognize-image
    → file_asset 저장
    → vision_recognition_request 레코드 생성 (status=PENDING)
    → requestId 즉시 반환

GET /api/v1/fridge/ingredients/recognize-image/status/{requestId}
    → vision_recognition_request 상태 조회
    → COMPLETED이면 analysis_result(JSONB) 반환

Background Worker (FastAPI)
    → status=PENDING 레코드 폴링
    → 이미지 전처리/인식 수행
    → analysis_result 업데이트, status=COMPLETED
```

`vision_recognition_request` 엔티티 컬럼: `file_id`, `vision_status`, `priority`, `analysis_result`(JSONB), `confidence_score`  
성능 인덱스: `(vision_status, priority, created_at)` 부분 인덱스 권장

---

### 2-8. shopping_item_mcp 캐시 운영 주의사항

- `unit_price_per_100` 컬럼에 100g/ml당 단위 가격 표준화 필수 (최저가 정렬 기준)
- `expires_at` 기준 캐시 만료 → `updated_at` 기준 오래된 데이터 주기적 삭제/갱신 스케줄링 필요
- `is_mcp_source` 플래그로 실시간 MCP 데이터 여부 구분

---

## 3. Spring Boot ↔ FastAPI 호출 흐름 요약

```
[클라이언트]
    ↓  GET /api/v1/shopping/lowest-price
[ShoppingController]  (신규 생성)
    ↓  ShoppingService.getLowestPrice()
[FastApiService.callShoppingLowestPrice()]  (신규 메서드 추가)
    ↓  POST http://localhost:8000/internal/v1/shopping/lowest-price
    ↓  헤더: X-Internal-Service, X-Internal-Token, X-Request-Id
[FastAPI — 민예린 구현 영역]
    ↓  MCP 호출 → 쇼핑몰 실시간 데이터
    ↓  shopping_item_mcp 캐시 저장
    ↓  응답 반환
[Spring Boot — shopping_item_mcp 저장 또는 직접 응답 조합]
    ↓
[클라이언트]
```
