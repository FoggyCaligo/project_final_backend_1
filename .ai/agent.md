# 에이전트 역할 정의 — 민예린

## 담당 범위

민예린은 **FastAPI 기반 AI/분석 백엔드(project_final_backend2)** 에서 다음 네 가지 영역을 전담한다.

1. **MCP 기반 외부 쇼핑 API 연동** — 부족 재료에 대해 쿠팡·네이버쇼핑 등 외부 쇼핑몰의 실시간 최저가를 MCP(Model Context Protocol)로 조회하고, 결과를 `shopping_item_mcp` 테이블에 캐싱한다.
2. **LLM / GraphRAG 모델링** — `ingredient_relation_graph` 테이블을 기반으로 식재료 간 대체·상관 관계를 GraphRAG로 추론하고, LLM이 생성한 추천 사유(`reason_text`)와 유사도(`similarity_score`)를 저장·제공한다.
3. **구형 기기 최적화 이미지 인식 파이프라인** — `vision_recognition_request` 테이블에 요청을 비동기로 적재하고, Background Worker에서 이미지 전처리(저사양 기기 대응)→ AI 인식→결과 저장까지의 파이프라인을 운영한다. 상태는 `vision_status`(PENDING / COMPLETED 등)로 추적된다.
4. **실시간 가격 비교 및 대체 상품 추천 로직** — `unit_price_per_100`(100g/ml당 환산가) 기준 최저가 정렬, GraphRAG 추론 기반 가성비 대체재 추천, 묶음 배송 최적화 제안을 구현한다.

---

## 담당 API

### 공개 API (Spring Boot → 클라이언트)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/shopping/lowest-price` | MCP 기반 특정 식재료 실시간 최저가 및 구매 링크 조회 |
| GET | `/api/v1/shopping/recipes/{recipeId}/missing-items` | 특정 레시피 부족 재료 묶음 최저가 장바구니 제안 |
| GET | `/api/v1/shopping/alternatives` | GraphRAG 기반 가성비 대체 식재료 추천 |
| GET | `/api/v1/fridge/ingredients/recognize-image/status/{requestId}` | (비동기) 이미지 분석 상태 및 결과 조회 |

> Spring Boot가 공개 API의 단일 진입점이며, 위 엔드포인트의 핵심 연산은 내부 API를 통해 FastAPI에 위임된다.

### 내부 API (Spring Boot → FastAPI, `/internal/v1`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/internal/v1/shopping/lowest-price` | MCP로 외부 쇼핑몰 실시간 최저가 조회 |
| POST | `/internal/v1/ai/graph-reasoning` | GraphRAG 기반 대체재 추천 및 묶음 배송 최적화 |
| POST | `/internal/v1/recommend/substitutions` | LLM 기반 부족 재료 대체 식재료 추천 |

모든 내부 API 호출에는 `X-Internal-Service`, `X-Internal-Token`, `X-Request-Id` 헤더가 필수이며, 브라우저에 직접 노출되지 않는다.

---

## 담당 DB 테이블

| 테이블 | 용도 |
|--------|------|
| `shopping_item_mcp` | MCP 수집 쇼핑 최저가 캐시 (`unit_price_per_100`, `expires_at` 포함) |
| `ingredient_relation_graph` | GraphRAG 추론 결과 — 대체·상관 관계, `similarity_score`, `reason_text` |
| `vision_recognition_request` | 비동기 이미지 인식 요청 상태 추적 (`vision_status`, `confidence_score`, `analysis_result`) |

---

## 핵심 구현 원칙

- **비동기 처리**: 이미지 인식 파이프라인은 트랜잭션과 분리된 Background Worker에서 처리한다.
- **캐시 만료 관리**: `shopping_item_mcp`의 `updated_at`을 기준으로 오래된 데이터를 주기적으로 삭제/최신화하는 스케줄링이 필요하다.
- **가성비 정렬 기준**: `unit_price_per_100` 필드에 100g/ml당 단위 가격을 표준화하여 저장해야 정확한 최저가 정렬이 가능하다.
- **GraphRAG 동기화**: 추론 결과는 배치로 `ingredient_relation_graph`에 동기화하여 읽기 성능을 확보한다.
- **보안**: 내부 API는 내부망 전용으로 운영하며, `X-Internal-Token`으로 인가한다.

---

## 기술 스택

- **언어/프레임워크**: Python, FastAPI
- **AI/ML**: LLM(추천 사유 생성), GraphRAG(식재료 관계 추론), 이미지 인식 모델
- **외부 연동**: MCP(쿠팡, 네이버쇼핑 등)
- **DB**: MariaDB (`shopping_item_mcp`, `ingredient_relation_graph`, `vision_recognition_request`)
- **저장소**: project_final_backend2
