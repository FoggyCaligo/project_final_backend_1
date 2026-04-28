# 에이전트 역할 정의 — 민예린

## 프로젝트 개요

- **팀 프로젝트** — 총 6인 협업
- **프로젝트명**: 오늘의 냉장고 (냉장고 재료 기반 맞춤 레시피 추천 서비스)
- **저장소**: `team_project_humaneducation` (Spring Boot 서버 + Next.js 프론트엔드 + FastAPI 모델 서버)

---

## 민예린 담당 범위

민예린은 **Spring Boot 백엔드** 및 **FastAPI 연동** 에서 다음 세 가지 영역을 전담한다.

### 1. 회원 로직
- 회원가입 (이메일 정규화, 비밀번호 검증, 중복 체크)
- 일반 로그인 / 로그아웃 (JWT 쿠키 기반, HTTP-only)
- 카카오 소셜 로그인 (추후 RestApiKey 발급 후 구현)
- 아이디/비밀번호 찾기

**담당 패키지**: `auth/`, `user/`

### 2. OCR 이미지 인식
- 식재료 이미지 업로드 → 비동기 OCR 처리 파이프라인
- `vision_recognition_request` 테이블로 상태(PENDING/PROCESSING/COMPLETED/FAILED) 추적
- FastAPI Background Worker에서 이미지 전처리(저사양 기기 대응) → AI 인식 → 결과 저장

**담당 API**:
- `POST /api/v1/fridge/ingredients/recognize-image` — 이미지 업로드 + 요청 생성
- `GET /api/v1/fridge/ingredients/recognize-image/status/{requestId}` — 처리 상태 및 결과 조회

### 3. 최저가 식재료 쇼핑 연동 MCP
- 부족 재료에 대해 쿠팡·네이버쇼핑 등 외부 쇼핑몰 실시간 최저가 조회 (MCP 기반)
- `shopping_item_mcp` 테이블에 결과 캐싱 (`unit_price_per_100` 기준 최저가 정렬)
- GraphRAG 기반 가성비 대체 식재료 추천
- 묶음 배송 최적화 제안

**담당 API**:
- `GET /api/v1/shopping/lowest-price`
- `GET /api/v1/shopping/recipes/{recipeId}/missing-items`
- `GET /api/v1/shopping/alternatives`

**담당 내부 API** (Spring Boot → FastAPI):
- `POST /internal/v1/shopping/lowest-price`
- `POST /internal/v1/ai/graph-reasoning`
- `POST /internal/v1/recommend/substitutions`

---

## Claude 작업 범위 및 제약

### Claude가 직접 실행하지 않는 것 (사용자가 직접 수행)
- **서버 실행** — `./gradlew bootRun`, `./mvnw spring-boot:run` 등
- **Git 명령어** — `git commit`, `git pull`, `git push`, `git merge`, `git rebase`, `git checkout` 등 모든 Git 작업
- **빌드 검증** — 서버 빌드 및 실행 확인은 사용자가 직접 수행

### Claude가 수행하는 것
- 코드 작성, 수정, 버그 분석
- 단위 테스트 코드 작성
- 파일 구조 설계 및 리뷰
- `.ai/` 문서 작성 및 업데이트

---

## 단위 테스트 원칙

**기능을 작성할 때마다 해당 기능의 단위 테스트를 함께 작성한다.**

| 레이어 | 테스트 대상 | 사용 도구 |
|--------|------------|---------|
| Service | 비즈니스 로직, 예외 분기 | JUnit 5 + Mockito |
| Controller | 요청/응답 포맷, HTTP 상태코드 | MockMvc |
| Repository | 쿼리 메서드 정확성 | `@DataJpaTest` |

테스트 파일 위치: `src/test/java/com/today/fridge/{도메인}/`

---

## 담당 DB 테이블

| 테이블 | 도메인 | 용도 |
|--------|--------|------|
| `users` | user | 회원 정보 |
| `user_session` | auth | JWT refresh token 해시 저장 |
| `vision_recognition_request` | ingredient | 비동기 이미지 인식 요청 상태 |
| `shopping_item_mcp` | shopping | MCP 수집 최저가 캐시 |
| `ingredient_relation_graph` | shopping | GraphRAG 대체 식재료 관계 |

---

## 팀원 연동 의존성

| 작업 | 의존하는 팀원 | 내용 |
|------|-------------|------|
| `ingredient_master_id` FK | 식재료 담당 팀원 | `ingredient_master` 테이블/엔티티 완성 필요 |
| `recipe_ingredient` 부족 재료 비교 | 레시피 담당 팀원 | 레시피 크롤링 데이터 적재 필요 |
| `file_asset` FK (이미지 인식) | 파일 업로드 담당 팀원 | `file_asset` 엔티티 UUID 처리 완성 필요 |
| JWT 인증 필터 공유 | 전체 팀 | SecurityConfig 변경 시 팀 공유 필요 |
| `ApiResponse` 공통 포맷 | 전체 팀 | 팀 합의 완료, 모든 응답에 적용 |

---

## 기술 스택

- **언어/프레임워크**: Java 17, Spring Boot 3.5, Spring Security
- **JWT**: io.jsonwebtoken:jjwt 0.12.6
- **DB**: PostgreSQL (JPA / Hibernate)
- **AI/ML 연동**: FastAPI (RestTemplate / WebClient)
- **외부 연동**: MCP (쿠팡, 네이버쇼핑 등)
- **테스트**: JUnit 5, Mockito, MockMvc
