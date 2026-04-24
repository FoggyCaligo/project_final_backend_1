# DB 스키마 적용 (대기)

## 현재 상태

- **`today_fridge_schema_only_v1.sql` 등 초안은 팀에서 수정 중**이므로, **수정본을 받기 전에는 DB에 실행하지 않는다.**
- 애플리케이션은 PostgreSQL + 스키마 `today_fridge` 기준으로 `application.yml`이 맞춰져 있다.

## 수정본 받은 뒤 절차 (체크리스트)

1. PostgreSQL에서 DB `today_fridge` 및 역할(예: `kool`) 준비 — 팀 가이드 따름.
2. 수정된 SQL만 `psql` 또는 DBeaver에서 실행.
3. 로컬 연결 정보를 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 환경변수 또는 `application.yml`에 맞출 것.
4. DDL이 단일 기준이면 **`spring.jpa.hibernate.ddl-auto`를 `validate` 또는 `none`으로 바꾸는 것을 권장** (`update`는 팀 스키마와 충돌할 수 있음).
5. 엔티티 컬럼이 DDL과 다르면 **엔티티·리포지토리를 DDL 기준으로 정합**할 것.

## 참고

- JDBC URL의 `currentSchema=today_fridge`와 `hibernate.default_schema`는 테이블이 `public`이 아닌 `today_fridge` 스키마에 있을 때를 위한 설정이다.
