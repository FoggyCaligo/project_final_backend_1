-- ===================================================================================
-- [0] 스키마 및 확장 모듈 초기화
-- ===================================================================================
CREATE SCHEMA IF NOT EXISTS today_fridge;
ALTER SCHEMA today_fridge OWNER TO postgres;

-- pgvector 확장 활성화 (ingredient_master의 벡터 검색용)
CREATE EXTENSION IF NOT EXISTS vector SCHEMA today_fridge;


-- ===================================================================================
-- [1] 커스텀 타입(ENUM) 생성
-- ===================================================================================
CREATE TYPE today_fridge.capture_source AS ENUM ('USER_MANUAL', 'CAMERA_AUTO', 'BATCH_UPLOAD');
CREATE TYPE today_fridge.cooking_context AS ENUM ('GENERAL', 'BAKING', 'FRYING', 'BOILING', 'GRILLING');
CREATE TYPE today_fridge.shipping_type AS ENUM ('FREE', 'STANDARD', 'EXPRESS', 'NEXT_DAY');
CREATE TYPE today_fridge.stock_status AS ENUM ('IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK');
CREATE TYPE today_fridge.substitute_source AS ENUM ('LLM', 'USER_SUGGESTION', 'EXPERT_REVIEWED');
CREATE TYPE today_fridge.user_ingredients_source_type AS ENUM ('MANUAL', 'IMAGE_RECOGNITION', 'CHATBOT_CONFIRM');
CREATE TYPE today_fridge.user_ingredients_storage_type AS ENUM ('ROOM', 'REFRIGERATED', 'FROZEN', 'ETC');
CREATE TYPE today_fridge.vision_status AS ENUM ('PENDING', 'UPSCALING', 'DETECTING', 'ANALYZING', 'COMPLETED', 'FAILED');


-- ===================================================================================
-- [2] 공통 트리거 함수 생성
-- ===================================================================================
CREATE OR REPLACE FUNCTION today_fridge.update_updated_at_column()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;
ALTER FUNCTION today_fridge.update_updated_at_column() OWNER TO postgres;


-- ===================================================================================
-- [3] 테이블 생성 (PK 및 기본 컬럼 구조 정의 / FK는 최하단에서 일괄 적용)
-- ===================================================================================

-- 1. 기본 유저 및 파일 관리
CREATE TABLE today_fridge.users (
    user_id BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    profile_image_url VARCHAR(2048),
    status VARCHAR(20),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.user_session (
    session_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(255),
    user_agent VARCHAR(255),
    last_ip VARCHAR(64),
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.refresh_tokens (
    refresh_token_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_agent TEXT,
    ip_address VARCHAR(45),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.user_follow (
    follow_id BIGSERIAL PRIMARY KEY,
    follower_user_id BIGINT NOT NULL,
    followee_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.file_asset (
    file_id BIGSERIAL PRIMARY KEY,
    uploader_user_id BIGINT NOT NULL,
    original_name VARCHAR(255),
    stored_name VARCHAR(255),
    storage_path VARCHAR(500),
    storage_type VARCHAR(20),
    mime_type VARCHAR(100),
    file_size BIGINT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. 마스터 데이터 (식재료, 카테고리, 상태 코드)
CREATE TABLE today_fridge.ingredient_categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.ingredient_category (
    category_id BIGSERIAL PRIMARY KEY,
    category_code VARCHAR(30) NOT NULL UNIQUE,
    category_name VARCHAR(50),
    is_active BOOLEAN,
    sort_order BIGINT
);

CREATE TABLE today_fridge.ingredient_master (
    ingredient_id BIGSERIAL PRIMARY KEY,
    category_id INTEGER,
    canonical_name VARCHAR(100) NOT NULL UNIQUE,
    normalized_name VARCHAR(100) NOT NULL UNIQUE,
    alias_text TEXT,
    standard_unit VARCHAR(10) DEFAULT 'g',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    embedding today_fridge.vector(1536),
    embedding_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON COLUMN today_fridge.ingredient_master.embedding IS 'OpenAI text-embedding-3-small';

CREATE TABLE today_fridge.ingredient_aliases (
    alias_id BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    alias_name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.condition_code (
    condition_id BIGSERIAL PRIMARY KEY,
    condition_group VARCHAR(30) NOT NULL,
    condition_code VARCHAR(50) NOT NULL UNIQUE,
    condition_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. 레시피 및 영양 정보
CREATE TABLE today_fridge.recipes (
    recipe_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200),
    summary TEXT,
    source_site VARCHAR(50),
    source_url VARCHAR(500),
    source_recipe_key VARCHAR(100),
    external_recipe_key VARCHAR(100),
    thumbnail_url VARCHAR(500),
    servings_text VARCHAR(50),
    serving_text VARCHAR(50),
    cook_time_text VARCHAR(50),
    cooking_time_text VARCHAR(50),
    difficulty_level VARCHAR(30),
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recipes_source_key UNIQUE (source_site, external_recipe_key)
);

CREATE TABLE today_fridge.recipe_nutrition (
    recipe_id BIGINT PRIMARY KEY,
    reference_weight NUMERIC(8, 2),
    calories NUMERIC(8, 2) NOT NULL,
    carbs NUMERIC(8, 2) NOT NULL,
    protein NUMERIC(8, 2) NOT NULL,
    fat NUMERIC(8, 2) NOT NULL,
    sugar NUMERIC(8, 2),
    sodium NUMERIC(8, 2),
    cholesterol NUMERIC(8, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.recipe_ingredients (
    recipe_ingredient_id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    ingredient_id BIGINT,
    raw_name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(100),
    quantity_text VARCHAR(100),
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.recipe_ingredient (
    recipe_ingredient_id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    ingredient_master_id BIGINT,
    raw_text VARCHAR(200),
    normalized_name_snapshot VARCHAR(100),
    amount_text VARCHAR(50),
    unit VARCHAR(20),
    is_optional BOOLEAN,
    sort_order BIGINT
);

CREATE TABLE today_fridge.recipe_steps (
    recipe_step_id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    step_no BIGINT NOT NULL,
    instruction_text TEXT NOT NULL,
    step_image_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.recipe_step (
    recipe_step_id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    step_no BIGINT,
    instruction_text TEXT,
    step_image_url VARCHAR(500)
);

CREATE TABLE today_fridge.recipe_condition_map (
    recipe_condition_map_id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    condition_id BIGINT NOT NULL,
    fit_type VARCHAR(20),
    source_type VARCHAR(20),
    confidence_score NUMERIC(5,2)
);

-- 4. 유저 활동, 식재료 및 식단
CREATE TABLE today_fridge.user_ingredients (
    user_ingredient_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ingredient_id BIGINT,
    category_id INTEGER,
    input_name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100),
    quantity NUMERIC(10,2),
    unit VARCHAR(20),
    storage_type today_fridge.user_ingredients_storage_type NOT NULL DEFAULT 'REFRIGERATED',
    purchased_at DATE,
    expiration_date DATE,
    memo VARCHAR(255),
    source_type today_fridge.user_ingredients_source_type NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.user_ingredient (
    user_ingredient_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ingredient_master_id BIGINT,
    raw_name VARCHAR(100),
    normalized_name_snapshot VARCHAR(100),
    quantity NUMERIC(10,2),
    unit VARCHAR(20),
    storage_type VARCHAR(30),
    freshness_status VARCHAR(20),
    expires_at DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.user_condition (
    user_condition_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    condition_id BIGINT NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.meal (
    meal_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_nutrition_id BIGINT NOT NULL,
    custom_food_name VARCHAR(100),
    servings NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    source_image_url VARCHAR(500),
    consumed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.day_nutrition (
    day_nutrition_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date TIMESTAMPTZ NOT NULL,
    total_calories NUMERIC(6, 2) NOT NULL,
    total_carbs NUMERIC(6, 2) NOT NULL,
    total_protein NUMERIC(6, 2) NOT NULL,
    total_fat NUMERIC(6, 2) NOT NULL,
    total_sugar NUMERIC(6, 2) NOT NULL,
    total_sodium NUMERIC(6, 2) NOT NULL,
    total_cholesterol NUMERIC(6, 2) NOT NULL
);

-- 5. 게시판, 피드백 및 소셜 활동
CREATE TABLE today_fridge.posts (
    post_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.post (
    post_id BIGSERIAL PRIMARY KEY,
    author_user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    like_count BIGINT,
    report_count BIGINT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.post_image (
    post_image_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sort_order BIGINT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.post_like (
    post_like_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.post_report (
    post_report_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    reporter_user_id BIGINT NOT NULL,
    reason_code VARCHAR(30),
    detail_text VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.bookmarks (
    bookmark_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_bookmarks_user_recipe UNIQUE (user_id, recipe_id)
);

CREATE TABLE today_fridge.bookmark (
    bookmark_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.notification (
    notification_id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    actor_user_id BIGINT,
    related_post_id BIGINT,
    notification_type VARCHAR(30),
    is_read BOOLEAN,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE today_fridge.recommendation_feedback (
    feedback_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recommendation_feedback UNIQUE (user_id, recipe_id, feedback_type, source_type)
);

-- 6. 백그라운드 AI 에이전트 작업
CREATE TABLE today_fridge.vision_recognition_request (
    request_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    status today_fridge.vision_status NOT NULL DEFAULT 'PENDING',
    priority SMALLINT NOT NULL DEFAULT 5 CHECK (priority BETWEEN 1 AND 9),
    device_info VARCHAR(100),
    capture_source today_fridge.capture_source NOT NULL DEFAULT 'USER_MANUAL',
    analysis_result JSONB,
    confidence_score NUMERIC(3,2) CHECK (confidence_score BETWEEN 0.00 AND 1.00),
    error_code VARCHAR(50),
    error_message VARCHAR(255),
    retry_count SMALLINT NOT NULL DEFAULT 0,
    max_retry SMALLINT NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE TABLE today_fridge.shopping_item_mcp (
    shopping_item_id BIGSERIAL PRIMARY KEY,
    ingredient_master_id BIGINT NOT NULL,
    mall_name VARCHAR(50) NOT NULL,
    mall_product_id VARCHAR(100),
    product_name VARCHAR(255) NOT NULL,
    brand VARCHAR(100),
    price INTEGER NOT NULL CHECK (price >= 0),
    original_price INTEGER CHECK (original_price >= 0),
    discount_rate NUMERIC(5,2),
    package_weight NUMERIC(10,2),
    package_unit VARCHAR(10),
    unit_price_per_100 NUMERIC(10,2),
    shipping_type today_fridge.shipping_type,
    shipping_fee INTEGER NOT NULL DEFAULT 0,
    stock_status today_fridge.stock_status NOT NULL DEFAULT 'IN_STOCK',
    rating NUMERIC(2,1) CHECK (rating BETWEEN 0.0 AND 5.0),
    review_count INTEGER NOT NULL DEFAULT 0,
    purchase_url TEXT NOT NULL,
    image_url TEXT,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mall_product UNIQUE (mall_name, mall_product_id)
);

CREATE TABLE today_fridge.substitute_graph (
    substitute_id BIGSERIAL PRIMARY KEY,
    base_ingredient_id BIGINT NOT NULL,
    sub_ingredient_id BIGINT NOT NULL,
    similarity_score NUMERIC(3,2) NOT NULL CHECK (similarity_score BETWEEN 0.00 AND 1.00),
    substitution_ratio NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    cooking_context today_fridge.cooking_context NOT NULL DEFAULT 'GENERAL',
    reason VARCHAR(500),
    source today_fridge.substitute_source NOT NULL DEFAULT 'LLM',
    confidence_level SMALLINT NOT NULL DEFAULT 3 CHECK (confidence_level BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sub_pair UNIQUE (base_ingredient_id, sub_ingredient_id, cooking_context),
    CONSTRAINT chk_not_self CHECK (base_ingredient_id <> sub_ingredient_id)
);


-- ===================================================================================
-- [4] 모든 소유권을 postgres로 설정
-- ===================================================================================
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'today_fridge') LOOP
        EXECUTE 'ALTER TABLE today_fridge.' || quote_ident(r.tablename) || ' OWNER TO postgres';
    END LOOP;
END $$;


-- ===================================================================================
-- [5] 인덱스 일괄 생성
-- ===================================================================================
-- pgvector 인덱스
CREATE INDEX idx_ingredient_master_embedding ON today_fridge.ingredient_master USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- 일반 인덱스 모음
CREATE INDEX idx_condition_code_group ON today_fridge.condition_code(condition_group);
CREATE INDEX idx_condition_code_active ON today_fridge.condition_code(is_active);
CREATE INDEX idx_user_condition_user ON today_fridge.user_condition(user_id);
CREATE INDEX idx_user_condition_condition ON today_fridge.user_condition(condition_id);
CREATE INDEX idx_user_condition_user_active ON today_fridge.user_condition(user_id, is_active);
CREATE INDEX idx_meal_user_date ON today_fridge.meal (user_id, consumed_at);
CREATE INDEX idx_recommendation_feedback_user_created ON today_fridge.recommendation_feedback(user_id, created_at);
CREATE INDEX idx_recommendation_feedback_recipe ON today_fridge.recommendation_feedback(recipe_id);
CREATE INDEX idx_recommendation_feedback_type ON today_fridge.recommendation_feedback(feedback_type);
CREATE INDEX idx_sim_ingredient_price ON today_fridge.shopping_item_mcp(ingredient_master_id, unit_price_per_100 ASC) WHERE stock_status = 'IN_STOCK';
CREATE INDEX idx_sim_expires ON today_fridge.shopping_item_mcp(expires_at);
CREATE INDEX idx_sim_ingredient_mall ON today_fridge.shopping_item_mcp(ingredient_master_id, mall_name);
CREATE INDEX idx_sub_base_score ON today_fridge.substitute_graph(base_ingredient_id, similarity_score DESC);
CREATE INDEX idx_sub_context ON today_fridge.substitute_graph(cooking_context, similarity_score DESC);
CREATE INDEX idx_sub_reverse ON today_fridge.substitute_graph(sub_ingredient_id, base_ingredient_id);
CREATE INDEX idx_vrr_user_created ON today_fridge.vision_recognition_request(user_id, created_at DESC);
CREATE INDEX idx_vrr_queue ON today_fridge.vision_recognition_request(status, priority, created_at) WHERE status IN ('PENDING', 'UPSCALING', 'DETECTING', 'ANALYZING');
CREATE INDEX idx_vrr_analysis_brand ON today_fridge.vision_recognition_request USING btree ((analysis_result->>'brand'));

-- 기존 덤프에 있던 검색 및 조회용 인덱스
CREATE INDEX idx_bookmarks_recipe_id ON today_fridge.bookmarks(recipe_id);
CREATE INDEX idx_ingredient_aliases_ingredient_id ON today_fridge.ingredient_aliases(ingredient_id);
CREATE INDEX idx_ingredient_master_category_id ON today_fridge.ingredient_master(category_id);
CREATE INDEX idx_notification_related_post_id ON today_fridge.notification(related_post_id);
CREATE INDEX idx_posts_title_content ON today_fridge.posts(title, content);
CREATE INDEX idx_posts_created_at ON today_fridge.posts(created_at);
CREATE INDEX idx_posts_recipe_id ON today_fridge.posts(recipe_id);
CREATE INDEX idx_posts_user_id ON today_fridge.posts(user_id);
CREATE INDEX idx_post_image_file_id ON today_fridge.post_image(file_id);
CREATE INDEX idx_post_image_post_id ON today_fridge.post_image(post_id);
CREATE INDEX idx_post_like_post_id ON today_fridge.post_like(post_id);
CREATE INDEX idx_post_report_post_id ON today_fridge.post_report(post_id);
CREATE INDEX idx_recipes_title_summary ON today_fridge.recipes(title, summary);
CREATE INDEX idx_recipes_published ON today_fridge.recipes(is_published);
CREATE INDEX idx_recipes_title ON today_fridge.recipes(title);
CREATE INDEX idx_recipe_condition_map_condition_id ON today_fridge.recipe_condition_map(condition_id);
CREATE INDEX idx_recipe_ingredients_ingredient_id ON today_fridge.recipe_ingredients(ingredient_id);
CREATE INDEX idx_recipe_ingredients_normalized_name ON today_fridge.recipe_ingredients(normalized_name);
CREATE INDEX idx_recipe_ingredients_recipe_sort ON today_fridge.recipe_ingredients(recipe_id, sort_order);
CREATE INDEX idx_recipe_steps_recipe_step_no ON today_fridge.recipe_steps(recipe_id, step_no);
CREATE INDEX idx_refresh_tokens_expires_at ON today_fridge.refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_user_id ON today_fridge.refresh_tokens(user_id);
CREATE INDEX idx_users_status ON today_fridge.users(status);
CREATE INDEX idx_user_ingredients_category ON today_fridge.user_ingredients(category_id);
CREATE INDEX idx_user_ingredients_expiration_date ON today_fridge.user_ingredients(expiration_date);
CREATE INDEX idx_user_ingredients_ingredient_id ON today_fridge.user_ingredients(ingredient_id);
CREATE INDEX idx_user_ingredients_normalized_name ON today_fridge.user_ingredients(normalized_name);
CREATE INDEX idx_user_ingredients_user_id ON today_fridge.user_ingredients(user_id);


-- ===================================================================================
-- [6] 타임스탬프 갱신 트리거 일괄 생성
-- ===================================================================================
CREATE TRIGGER trg_ingredient_master_updated_at BEFORE UPDATE ON today_fridge.ingredient_master FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_posts_updated_at BEFORE UPDATE ON today_fridge.posts FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_post_updated_at BEFORE UPDATE ON today_fridge.post FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_recipes_updated_at BEFORE UPDATE ON today_fridge.recipes FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_recipe_nutrition_updated_at BEFORE UPDATE ON today_fridge.recipe_nutrition FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_user_ingredients_updated_at BEFORE UPDATE ON today_fridge.user_ingredients FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_user_ingredient_updated_at BEFORE UPDATE ON today_fridge.user_ingredient FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON today_fridge.users FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_user_condition_updated_at BEFORE UPDATE ON today_fridge.user_condition FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_shopping_item_mcp_updated_at BEFORE UPDATE ON today_fridge.shopping_item_mcp FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();
CREATE TRIGGER trg_substitute_graph_updated_at BEFORE UPDATE ON today_fridge.substitute_graph FOR EACH ROW EXECUTE FUNCTION today_fridge.update_updated_at_column();


-- ===================================================================================
-- [7] 외래 키(FK) 참조 일괄 생성 (안전망)
-- ===================================================================================
ALTER TABLE today_fridge.ingredient_master ADD CONSTRAINT fk_ingredient_master_category FOREIGN KEY (category_id) REFERENCES today_fridge.ingredient_categories(category_id) ON DELETE SET NULL;
ALTER TABLE today_fridge.recipe_nutrition ADD CONSTRAINT fk_recipe_nutrition_recipe FOREIGN KEY (recipe_id) REFERENCES today_fridge.recipes(recipe_id) ON DELETE CASCADE;
ALTER TABLE today_fridge.recipe_condition_map ADD CONSTRAINT fk_recipe_condition_map_condition FOREIGN KEY (condition_id) REFERENCES today_fridge.condition_code(condition_id);
ALTER TABLE today_fridge.user_ingredients ADD CONSTRAINT fk_user_ingredients_category FOREIGN KEY (category_id) REFERENCES today_fridge.ingredient_categories(category_id) ON DELETE SET NULL;
ALTER TABLE today_fridge.user_condition ADD CONSTRAINT fk_user_condition_user FOREIGN KEY (user_id) REFERENCES today_fridge.users(user_id);
ALTER TABLE today_fridge.user_condition ADD CONSTRAINT fk_user_condition_condition FOREIGN KEY (condition_id) REFERENCES today_fridge.condition_code(condition_id);
ALTER TABLE today_fridge.meal ADD CONSTRAINT fk_meal_user FOREIGN KEY (user_id) REFERENCES today_fridge.users(user_id);
ALTER TABLE today_fridge.meal ADD CONSTRAINT fk_meal_recipe_nutrition FOREIGN KEY (recipe_nutrition_id) REFERENCES today_fridge.recipe_nutrition(recipe_id);
ALTER TABLE today_fridge.day_nutrition ADD CONSTRAINT fk_day_nutrition_user FOREIGN KEY (user_id) REFERENCES today_fridge.users(user_id);
ALTER TABLE today_fridge.post_image ADD CONSTRAINT fk_post_image_file FOREIGN KEY (file_id) REFERENCES today_fridge.file_asset(file_id);
ALTER TABLE today_fridge.post_image ADD CONSTRAINT fk_post_image_post FOREIGN KEY (post_id) REFERENCES today_fridge.post(post_id);
ALTER TABLE today_fridge.post_like ADD CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES today_fridge.post(post_id);
ALTER TABLE today_fridge.post_report ADD CONSTRAINT fk_post_report_post FOREIGN KEY (post_id) REFERENCES today_fridge.post(post_id);
ALTER TABLE today_fridge.notification ADD CONSTRAINT fk_notification_post FOREIGN KEY (related_post_id) REFERENCES today_fridge.post(post_id);
ALTER TABLE today_fridge.recommendation_feedback ADD CONSTRAINT fk_recommendation_feedback_user FOREIGN KEY (user_id) REFERENCES today_fridge.users(user_id);
ALTER TABLE today_fridge.recommendation_feedback ADD CONSTRAINT fk_recommendation_feedback_recipe FOREIGN KEY (recipe_id) REFERENCES today_fridge.recipes(recipe_id);
ALTER TABLE today_fridge.shopping_item_mcp ADD CONSTRAINT fk_sim_ingredient FOREIGN KEY (ingredient_master_id) REFERENCES today_fridge.ingredient_master(ingredient_id);
ALTER TABLE today_fridge.substitute_graph ADD CONSTRAINT fk_sub_base_ingredient FOREIGN KEY (base_ingredient_id) REFERENCES today_fridge.ingredient_master(ingredient_id);
ALTER TABLE today_fridge.substitute_graph ADD CONSTRAINT fk_sub_sub_ingredient FOREIGN KEY (sub_ingredient_id) REFERENCES today_fridge.ingredient_master(ingredient_id);
ALTER TABLE today_fridge.vision_recognition_request ADD CONSTRAINT fk_vrr_file FOREIGN KEY (file_id) REFERENCES today_fridge.file_asset(file_id);
ALTER TABLE today_fridge.vision_recognition_request ADD CONSTRAINT fk_vrr_user FOREIGN KEY (user_id) REFERENCES today_fridge.users(user_id);


-- ===================================================================================
-- [8] 코멘트 메타데이터
-- ===================================================================================
COMMENT ON TABLE today_fridge.shopping_item_mcp IS 'MCP 쇼핑 에이전트 실시간 가격 캐시';
COMMENT ON TABLE today_fridge.substitute_graph IS '식재료 대체재 관계 그래프';
COMMENT ON TABLE today_fridge.vision_recognition_request IS '비동기 이미지 인식 요청';
COMMENT ON COLUMN today_fridge.recipe_nutrition.reference_weight IS 'Unit: g';
COMMENT ON COLUMN today_fridge.recipe_nutrition.calories IS 'Unit: kcal';
COMMENT ON COLUMN today_fridge.recipe_nutrition.carbs IS 'Unit: g';
COMMENT ON COLUMN today_fridge.recipe_nutrition.protein IS 'Unit: g';
COMMENT ON COLUMN today_fridge.recipe_nutrition.fat IS 'Unit: g';
COMMENT ON COLUMN today_fridge.recipe_nutrition.sugar IS 'Unit: g';
COMMENT ON COLUMN today_fridge.recipe_nutrition.sodium IS 'Unit: mg';
COMMENT ON COLUMN today_fridge.recipe_nutrition.cholesterol IS 'Unit: mg';
COMMENT ON COLUMN today_fridge.day_nutrition.total_calories IS 'Unit: kcal';
COMMENT ON COLUMN today_fridge.day_nutrition.total_carbs IS 'Unit: g';
COMMENT ON COLUMN today_fridge.day_nutrition.total_protein IS 'Unit: g';
COMMENT ON COLUMN today_fridge.day_nutrition.total_fat IS 'Unit: g';
COMMENT ON COLUMN today_fridge.day_nutrition.total_sugar IS 'Unit: g';
COMMENT ON COLUMN today_fridge.day_nutrition.total_sodium IS 'Unit: mg';
COMMENT ON COLUMN today_fridge.day_nutrition.total_cholesterol IS 'Unit: mg';