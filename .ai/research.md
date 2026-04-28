# Research — 민예린 담당 영역 구현 참고

---

## 1. 현재 프로젝트 구조 요약

### 기술 스택 (실제 코드 기준)
- **백엔드**: Spring Boot 3.5, Java 17 (패키지: `com.today.fridge`)
- **DB**: PostgreSQL (`jdbc:postgresql://localhost:5435/today_fridge`)
- **JWT**: io.jsonwebtoken:jjwt 0.12.6 (HTTP-only 쿠키 방식)
- **FastAPI 연동**: `RestTemplate` (`AppConfig.java`에 Bean 등록)
- **파일 업로드**: 최대 단일 5MB, 요청 전체 25MB

### 도메인 패키지 구성

```
src/main/java/com/today/fridge/
├── auth/
│   ├── controller/  AuthController.java (login/logout), KakaoAuthController.java (stub)
│   ├── dto/request/ LoginRequest.java
│   ├── entity/      UserSession.java
│   ├── repository/  UserSessionRepository.java
│   ├── security/    JwtProvider.java, JwtAuthenticationFilter.java
│   └── service/     AuthService.java, CustomUserDetailsService.java
├── user/
│   ├── controller/  UserController.java (signup, find-loginid)
│   ├── dto/request/ SignupRequest.java
│   ├── entity/      User.java, UserFollow.java, Notification.java
│   ├── repository/  UserRepository.java, UserFollowRepository.java, NotificationRepository.java
│   └── service/     UserService.java
├── global/
│   ├── config/      AppConfig.java, SecurityConfig.java, WebMvcConfig.java
│   ├── exception/   ErrorCode.java, ExceptionTemplate.java, GlobalExceptionHandler.java
│   ├── filter/      MDCLoggingFilter.java
│   ├── response/    ApiResponse.java, ApiResponseUUID.java
│   └── external/fastapi/ FastApiService.java
├── ingredient/      (식재료 엔티티 구현됨, Controller는 스텁)
├── recipe/          (레시피 엔티티 구현됨, Controller는 스텁)
├── shopping/        (미구현 — Phase 2 대상)
└── ...
```

---

## 2. 팀 공식 스펙 반영 현황 (2026-04-28 확인)

### 2-0. 현행 구현 vs 팀 공식 API 스펙 대조

| 구분 | 팀 공식 경로 | 현행 구현 경로 | 상태 |
|------|------------|--------------|------|
| 회원가입 | `POST /api/v1/auth/signup` | `POST /api/v1/users/signup` | ⚠️ 경로 불일치 — 이동 필요 |
| 로그인 | `POST /api/v1/auth/login` | `POST /api/v1/auth/login` | ✅ |
| 로그아웃 | `POST /api/v1/auth/logout` | `POST /api/v1/auth/logout` | ✅ |
| CSRF 토큰 발급 | `GET /api/v1/auth/csrf-token` | 미구현 | ❌ |
| 아이디 중복 확인 | `GET /api/v1/auth/check-login-id` | 미구현 (Service 메서드만 존재) | ❌ |
| Refresh Token 재발급 | `POST /api/v1/auth/refresh` | 미구현 | ❌ |
| 현재 사용자 조회 | `GET /api/v1/auth/me` | 미구현 | ❌ |
| 마이페이지 조회 | `GET /api/v1/users/me/profile` | 미구현 | ❌ |
| 마이페이지 수정 | `PATCH /api/v1/users/me/profile` | 미구현 | ❌ |
| 비밀번호 변경 | `PATCH /api/v1/users/me/password` | 미구현 | ❌ |
| 아이디 찾기 | (팀 스펙 미포함) | `GET /api/v1/users/find-loginid` | ℹ️ 팀 재확인 필요 |

### 2-0-1. user_session 테이블 팀 공식 스펙 vs 현행 엔티티

| 컬럼 | 팀 공식 스펙 | 현행 엔티티 | 비고 |
|------|-----------|----------|------|
| `session_id` | BIGINT PK | ✅ | |
| `user_id` | BIGINT FK | ✅ | |
| `refresh_token_hash` | VARCHAR(255) | ✅ (refreshToken 컬럼명 확인 필요) | 원문 아닌 해시 저장 원칙 |
| `user_agent` | VARCHAR(255) | ❌ 미추가 | 클라이언트 식별 보조 |
| `last_ip` | VARCHAR(64) | ❌ 미추가 | 최근 접속 IP |
| `expires_at` | TIMESTAMPTZ | ✅ | |
| `revoked_at` | TIMESTAMPTZ NULL | ❌ 미추가 | 무효화 시각 (로그아웃) |
| `created_at` | TIMESTAMPTZ | ✅ | |

### 2-0-2. 인증·보안 정책 팀 확정 사항

- **CSRF**: 상태 변경 요청(POST/PATCH/DELETE)에 `X-CSRF-Token` 헤더 필수
  - `GET /api/v1/auth/csrf-token` → CSRF 토큰 발급 + 쿠키 동기화
- **SameSite=Lax** 기본, HTTPS 배포 시 `Secure` 속성 추가
- **토큰 저장 금지**: 브라우저 `localStorage` / `sessionStorage` 에 JWT 원문 저장 금지 (현행 AuthContext는 loginId+loginType 요약 정보만 저장 — 정책 준수)
- **refreshToken**: 원문 저장 금지, `user_session.refresh_token_hash`에 해시만 저장

### 2-0-3. users 테이블 팀 공식 스펙

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `user_id` | BIGINT PK | |
| `login_id` | VARCHAR(50) UNIQUE | |
| `password_hash` | VARCHAR(255) | |
| `nickname` | VARCHAR(50) | |
| `status` | VARCHAR(20) | ACTIVE / INACTIVE |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

⚠️ 팀 공식 스펙에 `email` 컬럼이 없음. 현행 User 엔티티에는 email 존재. **팀 내 확인 필요** (아이디 찾기 기능에 email 사용 중).

---

## 3. 작업 이력

---

### [2026-04-28] 회원 로직 초기 구현 (팀 공식 스펙 반영 전 구현)

#### 2-1. 버그 수정: JwtProvider.java — JJWT 0.12.x deprecated API 교체

**문제**: `build.gradle`에 jjwt `0.12.6` 사용 중이었으나 코드가 `0.11.x` 스타일 API를 혼용하여 컴파일 경고 및 런타임 오류 발생 가능.

| 구버전 (deprecated) | 신버전 (0.12.x) |
|---|---|
| `java.security.Key` | `javax.crypto.SecretKey` |
| `Jwts.parserBuilder().setSigningKey(key).build()` | `Jwts.parser().verifyWith(key).build()` |
| `.parseClaimsJws(token)` | `.parseSignedClaims(token)` |
| `.getBody()` | `.getPayload()` |
| `.setSubject()` / `.setIssuedAt()` / `.setExpiration()` | `.subject()` / `.issuedAt()` / `.expiration()` |
| `.signWith(key, SignatureAlgorithm.HS256)` | `.signWith(key)` |

**파일**: `auth/security/JwtProvider.java`

---

#### 2-2. 신규 구현: 회원가입 / 아이디 찾기

**추가된 파일 목록**

| 파일 | 내용 |
|------|------|
| `user/dto/request/SignupRequest.java` | Bean Validation 포함 회원가입 요청 DTO |
| `user/service/UserService.java` | 회원가입, 이메일 정규화, 비밀번호 검증, 아이디 찾기 |
| `user/controller/UserController.java` | POST /signup, GET /find-loginid |

**수정된 파일 목록**

| 파일 | 변경 내용 |
|------|---------|
| `user/entity/User.java` | `User.create()` static factory method 추가, `@PrePersist` / `@PreUpdate` 추가 |
| `user/repository/UserRepository.java` | `findByEmail`, `existsByLoginId`, `existsByEmail`, `existsByNickname` 추가 |
| `global/exception/ErrorCode.java` | `DUPLICATE_LOGIN_ID`, `DUPLICATE_NICKNAME` 추가 |

**구현 API**

| HTTP | 경로 | 설명 |
|------|------|------|
| `POST` | `/api/v1/users/signup` | 회원가입 (loginId/email/password/nickname) |
| `GET` | `/api/v1/users/find-loginid?email=` | 이메일로 아이디 찾기 |

**UserService 내부 로직**

| 메서드 | 설명 |
|--------|------|
| `signup(SignupRequest)` | 이메일 정규화 → 비밀번호 검증 → 중복 체크(loginId/email/nickname) → BCrypt 해싱 → 저장 |
| `normalizeEmail(String)` | `trim()` + `toLowerCase()` |
| `validatePassword(String)` | 영문/숫자/특수문자 각 1자 이상, 8~30자 — 불충족 시 `INVALID_INPUT_VALUE` 예외 |
| `findLoginIdByEmail(String)` | 이메일로 사용자 조회 → loginId 반환 — 없으면 `USER_NOT_FOUND` 예외 |

**SignupRequest 검증 규칙**

| 필드 | 제약 |
|------|------|
| `loginId` | `@NotBlank`, 4~20자, `^[a-zA-Z0-9_]+$` |
| `email` | `@NotBlank`, `@Email` |
| `password` | `@NotBlank`, 8~30자, 영문+숫자+특수문자 각 1자 이상 |
| `nickname` | `@NotBlank`, 2~20자 |

---

#### 2-3. 단위 테스트 작성 완료 [2026-04-28]

**추가 수정**: `SignupRequest.java`에 `@Setter` 추가 — Jackson 역직렬화 시 private 필드 주입 불가 버그 수정

| 클래스 | 파일 | 테스트 케이스 수 |
|--------|------|----------------|
| `UserService` | `user/service/UserServiceTest.java` | 13개 |
| `UserController` | `user/controller/UserControllerTest.java` | 6개 |
| `JwtProvider` | `auth/security/JwtProviderTest.java` | 9개 |

**UserServiceTest 케이스**

| 메서드 | 시나리오 |
|--------|---------|
| `signup` | 정상, loginId 중복, email 중복, nickname 중복, 비밀번호 짧음, 특수문자 없음, 숫자 없음, 영문 없음 |
| `normalizeEmail` | 대소문자+공백 정규화, null 입력 |
| `findLoginIdByEmail` | 정상, USER_NOT_FOUND, 대소문자 무관 조회 |

**UserControllerTest 케이스**

| 엔드포인트 | 시나리오 |
|-----------|---------|
| `POST /signup` | 정상 success:true, loginId 공백 error, DUPLICATE_LOGIN_ID, DUPLICATE_EMAIL |
| `GET /find-loginid` | 정상 loginId 반환, USER_NOT_FOUND |

**JwtProviderTest 케이스**

| 대상 | 시나리오 |
|------|---------|
| `createAccessToken` / `createRefreshToken` | 비어있지 않은 문자열 반환 |
| `validateToken` | 유효, 만료(validity=-1000ms), 잘못된 형식, 빈 문자열 |
| `getLoginIdFromToken` | loginId 정확히 추출 |
| `createTokenCookie` | HttpOnly/Secure/maxAge 속성 검증 |
| `resolveTokenFromCookie` | 쿠키 추출, 없을 때 null |

---

## 4. 다음 구현 우선순위 (팀 스펙 기준)

| 순서 | 작업 | 파일 | 비고 |
|------|------|------|------|
| 1 | `UserSession` 엔티티에 `user_agent`, `last_ip`, `revoked_at` 컬럼 추가 | `auth/entity/UserSession.java` | `revoked_at` NULL 허용 |
| 2 | 회원가입 API 경로 이동: `UserController` → `AuthController` | `auth/controller/AuthController.java` | `/api/v1/auth/signup` |
| 3 | `GET /api/v1/auth/check-login-id` 구현 | `auth/controller/AuthController.java` | loginId 중복 확인 |
| 4 | `GET /api/v1/auth/csrf-token` 구현 | `auth/controller/AuthController.java` | CSRF 쿠키 동기화 |
| 5 | `POST /api/v1/auth/refresh` 구현 | `auth/controller/AuthController.java` | refreshToken 해시 검증 + 재발급 |
| 6 | `GET /api/v1/auth/me` 구현 | `auth/controller/AuthController.java` | SecurityContext에서 loginId 추출 |
| 7 | `GET/PATCH /api/v1/users/me/profile` 구현 | `user/controller/UserController.java` | 마이페이지 |
| 8 | `PATCH /api/v1/users/me/password` 구현 | `user/controller/UserController.java` | 비밀번호 변경 |
| 9 | `SecurityConfig` 인증 분기 적용 | `global/config/SecurityConfig.java` | 인증 필요 API 보호 |
| 10 | users 테이블 email 컬럼 여부 팀 확인 후 처리 | — | 아이디 찾기 기능 영향 |

---

## 5. 기존 인프라 참고사항

### 5-1. FastApiService 확장 방법

현재 `FastApiService.java`는 `health()`만 존재한다. OCR 인식 및 쇼핑 연동 메서드를 이 클래스에 추가하거나 기능별 별도 클래스로 분리한다.

**application.yml FastAPI 연동 설정값**:
```yaml
app:
  fastapi:
    base-url: http://localhost:8000
    service-key: change-me
    caller-service: spring-backend
```

내부 API 호출 시 의무 헤더 3종:
```
X-Internal-Service: spring-backend
X-Internal-Token:   change-me
X-Request-Id:       {UUID}
```

---

### 5-2. 미구현 엔티티 (Phase 2 대상)

| 테이블 | 생성 위치 | 비고 |
|--------|---------|------|
| `vision_recognition_request` | `ingredient/entity/` | `file_id` → `file_asset` FK, `analysis_result` JSONB |
| `shopping_item_mcp` | `shopping/entity/` | `unit_price_per_100`, `expires_at` 포함 |
| `ingredient_relation_graph` | `shopping/entity/` | self-join (`base_ingredient_id`, `target_ingredient_id`) |

---

### 5-3. SecurityConfig 현황

현재 **모든 요청 `permitAll()`** 처리 중. 쇼핑 API 3종은 인증 필요 API이므로 회원 로직 완성 후 인증 조건 추가 예정.

---

### 5-4. ApiResponse 공통 응답 형식

`global/response/ApiResponse.java`가 구현 완료된 상태. 모든 응답에 사용.

```json
{
  "success": true,
  "code": "200",
  "message": "...",
  "requestId": "req_xxx",
  "data": { ... }
}
```

---

### 3-5. 카카오 로그인 (KakaoAuthController) — 미구현

- 팀 내 카카오 개발자 계정 생성 필요
- RestApiKey `.env` 설정 필요
- 유저에서 받아오는 값 (이메일, 닉네임 등) 팀 내 협의 필요
- 파일: `auth/controller/KakaoAuthController.java` (현재 주석 스텁만 존재)

---

### 3-6. AuthController 추후 개선 항목

- Redis 기반 token / refreshToken 관리로 교체
- refreshToken 30분마다 재발급 로직 추가
