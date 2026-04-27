# 구현 계획 — 민예린 담당 영역

> 기준: docs.txt 요구사항 + research.md 분석 결과  
> 저장소: project_final_backend1 (Spring Boot) + project_final_backend2 (FastAPI)

---

## 전체 구현 순서 개요

```
Phase 1. 공통 인프라 준비          (다른 팀원 작업과 병행 가능)
Phase 2. 엔티티 & 리포지토리 생성   (DB 스키마 확정 후)
Phase 3. FastAPI 내부 API 구현     (backend2)
Phase 4. Spring Boot 서비스/컨트롤러 구현  (backend1)
Phase 5. 연동 테스트 & 마무리
```

---

## Phase 1. 공통 인프라 준비

### 1-1. ApiResponse 공통 응답 클래스 구현
- **파일**: `global/response/ApiResponse.java` (현재 빈 클래스)
- **구현 내용**:
  ```java
  // 제네릭 공통 응답 래퍼
  // 필드: success(boolean), code(String), message(String), data(T), requestId(String)
  // 정적 팩토리: ApiResponse.ok(data), ApiResponse.fail(code, message)
  ```
- **협의 필요**: 다른 팀원도 동일하게 사용하므로 팀 전체와 포맷 합의 후 작성

### 1-2. RestTemplate 타임아웃 설정
- **파일**: `global/config/AppConfig.java`
- **이유**: MCP 외부 쇼핑몰 호출 시 응답 지연 대비
- **구현 내용**:
  ```java
  // HttpComponentsClientHttpRequestFactory 사용
  // connectTimeout: 3초, readTimeout: 10초
  ```

### 1-3. FastApiService 내부 API 호출 공통 헤더 처리
- **파일**: `global/external/fastapi/FastApiService.java`
- **구현 내용**: 모든 내부 API 호출에 아래 헤더 자동 주입하는 헬퍼 메서드 추가
  ```
  X-Internal-Service: ${app.fastapi.caller-service}
  X-Internal-Token:   ${app.fastapi.service-key}
  X-Request-Id:       UUID.randomUUID()
  ```

---

## Phase 2. 엔티티 & 리포지토리 생성 (Spring Boot)

### 2-1. ShoppingItemMcp 엔티티
- **파일**: `shopping/entity/ShoppingItemMcp.java`
- **테이블**: `shopping_item_mcp`
- **핵심 컬럼**:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `shopping_item_mcp_id` | Long (PK) | |
| `ingredient_master_id` | Long (FK) | `ingredient_master` 참조 |
| `mall_name` | String | 쿠팡, 네이버 등 |
| `price` | BigDecimal | 판매 가격 |
| `unit_price_per_100` | BigDecimal | 100g/ml당 환산가 **(최저가 정렬 핵심)** |
| `stock_status` | String | 재고 상태 |
| `purchase_url` | String | 상품 딥링크 |
| `is_mcp_source` | Boolean | MCP 실시간 데이터 여부 |
| `expires_at` | LocalDateTime | 캐시 만료 시각 |
| `updated_at` | LocalDateTime | 갱신 시각 (스케줄링 기준) |

- **리포지토리**: `shopping/repository/ShoppingItemMcpRepository.java`
  - `findByIngredientMasterIdAndExpiresAtAfter()` — 유효한 캐시 조회
  - `deleteByUpdatedAtBefore()` — 만료 캐시 삭제 (스케줄링용)

### 2-2. IngredientRelationGraph 엔티티
- **파일**: `shopping/entity/IngredientRelationGraph.java`
- **테이블**: `ingredient_relation_graph`
- **핵심 컬럼**:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `relation_id` | Long (PK) | |
| `base_ingredient_id` | Long (FK) | 원재료 (`ingredient_master` 참조) |
| `target_ingredient_id` | Long (FK) | 대체 재료 (`ingredient_master` 참조) |
| `relation_type` | String | SUBSTITUTE / RELATED |
| `similarity_score` | Double | 조리 유사도 (0~1) |
| `substitution_ratio` | String | 대체 비율 |
| `cooking_context` | String | 적용 조리법 |
| `reason_text` | String | LLM 생성 추천 사유 |

- **리포지토리**: `shopping/repository/IngredientRelationGraphRepository.java`
  - `findByBaseIngredientId()` — 특정 재료의 대체재 목록 조회

### 2-3. VisionRecognitionRequest 엔티티
- **파일**: `ingredient/entity/VisionRecognitionRequest.java`
- **테이블**: `vision_recognition_request`
- **핵심 컬럼**:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `request_id` | Long (PK) | |
| `user_id` | Long (FK) | 요청 사용자 |
| `file_id` | Long (FK) | `file_asset` 참조 |
| `vision_status` | String | PENDING / PROCESSING / COMPLETED / FAILED |
| `priority` | Integer | 처리 우선순위 |
| `analysis_result` | String (JSON) | AI 인식 결과 (PostgreSQL JSONB) |
| `confidence_score` | Double | 인식 신뢰도 |
| `created_at` | LocalDateTime | |
| `completed_at` | LocalDateTime | |

- **리포지토리**: `ingredient/repository/VisionRecognitionRequestRepository.java`
  - `findByRequestIdAndUserId()` — 상태 조회
  - 인덱스: `(vision_status, priority, created_at)` 부분 인덱스 — DDL 또는 `@Index` 어노테이션으로 설정

---

## Phase 3. FastAPI 내부 API 구현 (project_final_backend2)

### 3-1. POST /internal/v1/shopping/lowest-price
- **역할**: MCP를 통해 외부 쇼핑몰(쿠팡, 네이버쇼핑 등) 실시간 최저가 조회
- **요청**: `items[{name, min_amount}]`, `user_pref{가성비/배송속도/특정몰}`
- **응답**: `shopping_results[{item_name, mall_name, price, unit_price, purchase_url, is_mcp_source}]`
- **구현 포인트**:
  - MCP 연동 로직 핵심 구현 위치
  - `unit_price` 계산 시 단위 표준화 (100g 또는 100ml 기준) 필수
  - MCP 응답 실패 시 캐시(`shopping_item_mcp`) 폴백 처리

### 3-2. POST /internal/v1/ai/graph-reasoning
- **역할**: GraphRAG로 비싼/품절 재료의 대체재 추천 및 묶음 배송 최적화
- **요청**: `target_ingredient(string)`, `context(string, 레시피 정보)`
- **응답**: `alternative_item[{name, reason, similarity_score, price_advantage}]`
- **구현 포인트**:
  - `ingredient_relation_graph` 데이터를 GraphRAG 추론에 활용
  - `reason` 문구는 LLM이 생성 (예: "시금치 가격 급등으로 청경채 추천, 영양성분 85% 일치")
  - 추론 결과는 배치로 DB에 동기화하여 읽기 성능 확보

### 3-3. POST /internal/v1/recommend/substitutions
- **역할**: 레시피 맥락 + 보유 식재료 기반 LLM 대체 재료 추천
- **요청**: `missingIngredients`, `ownedIngredients`, `recipeContext`, `userConditionTags`, `allowExternalSuggestion`
- **응답**: `substitutions[{originalName, alternativeName, reason, confidence, warning?}]`
- **구현 포인트**:
  - `sourceType`: `owned`(보유 재료 중 대체) vs `generic`(일반 대체안)
  - `userConditionTags`(다이어트, 알레르기 등) 반드시 필터링에 반영
  - `warning` 필드로 알레르기/풍미 차이 주의 문구 제공

### 3-4. 비동기 이미지 인식 Background Worker
- **역할**: `vision_recognition_request` 테이블의 PENDING 레코드를 폴링하여 처리
- **흐름**:
  1. `vision_status = PENDING` 레코드 조회 (`priority` 오름차순)
  2. 구형 기기 대응 이미지 전처리 (리사이즈, 품질 조정)
  3. AI 모델로 식재료 인식
  4. `analysis_result` 업데이트, `vision_status = COMPLETED`
- **구현 포인트**:
  - 트랜잭션과 분리된 Background Worker로 구성 (FastAPI BackgroundTasks 또는 Celery)
  - 처리 중 상태를 `PROCESSING`으로 변경하여 중복 처리 방지
  - 실패 시 `FAILED` + 재시도 횟수 관리

---

## Phase 4. Spring Boot 서비스/컨트롤러 구현 (project_final_backend1)

### 4-1. FastApiService 메서드 추가
- **파일**: `global/external/fastapi/FastApiService.java`
- 추가할 메서드:
  ```java
  ShoppingLowestPriceResponse callShoppingLowestPrice(ShoppingLowestPriceRequest request)
  GraphReasoningResponse callGraphReasoning(GraphReasoningRequest request)
  SubstitutionResponse callSubstitutions(SubstitutionRequest request)
  ```

### 4-2. ShoppingService 구현
- **파일**: `shopping/service/ShoppingService.java`
- **메서드별 로직**:

  **getLowestPrice(ingredientName, userPref)**
  1. `shopping_item_mcp` 유효 캐시 조회
  2. 캐시 없으면 FastAPI `/internal/v1/shopping/lowest-price` 호출
  3. 결과를 `shopping_item_mcp`에 저장 후 반환
  4. `unit_price_per_100` 오름차순 정렬하여 응답

  **getMissingItemsForRecipe(recipeId, userId)**
  1. `recipe_ingredient`에서 해당 레시피 재료 목록 조회
  2. `user_ingredient`에서 사용자 보유 재료 조회
  3. `ingredient_master_id` 기준 비교 → 부족 재료 목록 추출
  4. 부족 재료 목록으로 `getLowestPrice` 묶음 호출

  **getAlternatives(ingredientName)**
  1. `ingredient_relation_graph`에서 대체재 조회
  2. 없으면 FastAPI `/internal/v1/ai/graph-reasoning` 호출
  3. 결과를 `ingredient_relation_graph`에 배치 동기화 후 반환

### 4-3. ShoppingController 구현
- **파일**: `shopping/controller/ShoppingController.java`
- **엔드포인트**:
  ```
  GET /api/v1/shopping/lowest-price?ingredient={name}
  GET /api/v1/shopping/recipes/{recipeId}/missing-items
  GET /api/v1/shopping/alternatives?ingredient={name}
  ```
- 모두 인증 필요 → JWT 인증 필터 완성 시 보안 설정 추가

### 4-4. 비동기 이미지 인식 상태 조회 API 추가
- **파일**: `ingredient/controller/UserIngredientController.java`
- **엔드포인트 추가**:
  ```
  POST /api/v1/fridge/ingredients/recognize-image
      → file_asset 저장
      → VisionRecognitionRequest 생성 (status=PENDING)
      → requestId 반환

  GET /api/v1/fridge/ingredients/recognize-image/status/{requestId}
      → VisionRecognitionRequest 상태 조회
      → COMPLETED 시 analysis_result 파싱 후 재료 후보 목록 반환
  ```

### 4-5. 캐시 만료 스케줄러
- **파일**: `shopping/service/ShoppingCacheScheduler.java`
- **역할**: `shopping_item_mcp`의 만료된 캐시 데이터 주기적 삭제
  ```java
  @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
  public void cleanExpiredShoppingCache() { ... }
  ```

---

## Phase 5. 연동 테스트 & 마무리

### 5-1. Spring Boot ↔ FastAPI 연동 확인
- [ ] FastAPI 서버 실행 후 `/api/fastapi-test` 헬스체크 통과 확인
- [ ] 내부 API 3종 (`/internal/v1/shopping/lowest-price`, `/internal/v1/ai/graph-reasoning`, `/internal/v1/recommend/substitutions`) 수동 호출 확인
- [ ] `X-Internal-Token` 불일치 시 401 반환 확인

### 5-2. 쇼핑 API 흐름 확인
- [ ] `GET /api/v1/shopping/lowest-price` — MCP 결과 반환 및 캐시 저장 확인
- [ ] `GET /api/v1/shopping/recipes/{recipeId}/missing-items` — 부족 재료 계산 정확성 확인
- [ ] `GET /api/v1/shopping/alternatives` — GraphRAG 대체재 반환 확인
- [ ] `unit_price_per_100` 기준 정렬 정확성 확인

### 5-3. 이미지 인식 비동기 흐름 확인
- [ ] 이미지 업로드 → `PENDING` 레코드 생성 확인
- [ ] Background Worker 처리 후 `COMPLETED` 전환 확인
- [ ] 상태 조회 API에서 `analysis_result` 정상 반환 확인

---

## 팀원 연동 의존성

| 작업 | 의존하는 팀원 | 내용 |
|------|-------------|------|
| `ingredient_master_id` FK 사용 | 장민재 | `ingredient_master` 테이블/엔티티 완성 필요 |
| `recipe_ingredient` 부족 재료 비교 | 신재용 | 레시피 크롤링으로 `recipe_ingredient` 데이터 적재 필요 |
| `file_asset` FK (이미지 인식) | 정경안 | `file_asset` 엔티티 및 UUID 처리 완성 필요 |
| JWT 인증 필터 | 임동주 | 쇼핑 API 3종 인증 처리는 인증 필터 완성 후 적용 |
| `ApiResponse` 공통 포맷 | 전체 팀 | 팀 합의 후 구현, 이후 모든 응답에 적용 |
