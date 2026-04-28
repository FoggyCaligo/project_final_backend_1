package com.today.fridge.global.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.ingredient.entity.IngredientCategory;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.repository.IngredientCategoryRepository;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 개발(local) 환경에서 테스트용 유저 및 카테고리 기준 데이터가 없을 경우 자동으로 생성합니다.
 * 운영(prod) 환경에서는 동작하지 않습니다.
 */
@Component
@Profile("local")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final ObjectMapper objectMapper;

    public DevDataInitializer(UserRepository userRepository,
                              IngredientCategoryRepository ingredientCategoryRepository,
                              IngredientMasterRepository ingredientMasterRepository,
                              ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.ingredientMasterRepository = ingredientMasterRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUser();
        seedCategories();
        seedIngredientMasterFromCanonicalGroceryFile();
    }

    private void seedUser() throws Exception {
        if (userRepository.count() == 0) {
            User user = new User();
            setField(user, "loginId", "testuser");
            setField(user, "email", "testuser@test.com");
            setField(user, "passwordHash", "$2a$10$devtesthashplaceholderonly00000");
            setField(user, "nickname", "테스트유저");
            setField(user, "status", "ACTIVE");
            setField(user, "createdAt", LocalDateTime.now());
            setField(user, "updatedAt", LocalDateTime.now());
            userRepository.save(user);
            log.info("[DevDataInitializer] 테스트 유저 생성 완료 (loginId=testuser)");
        } else {
            log.info("[DevDataInitializer] 기존 유저 존재, 초기화 생략");
        }
    }

    private void seedCategories() {
        if (ingredientCategoryRepository.count() > 0) {
            log.info("[DevDataInitializer] 카테고리 데이터 존재, 시드 생략");
            return;
        }
        List<IngredientCategory> categories = List.of(
                category("VEGETABLE", "채소",   true, 1),
                category("FRUIT",     "과일",   true, 2),
                category("MEAT",      "육류",   true, 3),
                category("SEAFOOD",   "해산물", true, 4),
                category("DAIRY",     "유제품", true, 5),
                category("GRAIN",     "곡류",   true, 6),
                category("SEASONING", "조미료", true, 7),
                category("SAUCE",     "소스",   true, 8),
                category("ETC",       "기타",   true, 99)
        );
        ingredientCategoryRepository.saveAll(categories);
        log.info("[DevDataInitializer] ingredient_category 시드 데이터 {} 건 삽입 완료", categories.size());
    }

    /**
     * {@code data/grocery_ingredient_master_seed.json} — 원본 grocery 매핑을 팀 규격에 맞게 정리한 시드.
     * <ul>
     *   <li>{@code canonical_name} / {@code normalized_name}: DDL·ERD 기준 표준 영문명(동일 값 삽입, 유일)</li>
     *   <li>{@code category_id}: {@code ingredient_category.category_code}로 조회한 FK</li>
     *   <li>{@code alias_text}: {@code ko:한글별칭…|src:mapping_grocery_dataset|key:원본키|type:원본분류}</li>
     * </ul>
     * 동일 {@code normalized_name}이 이미 있으면 건너뜁니다.
     */
    private void seedIngredientMasterFromCanonicalGroceryFile() throws Exception {
        ClassPathResource resource = new ClassPathResource("data/grocery_ingredient_master_seed.json");
        if (!resource.exists()) {
            log.warn("[DevDataInitializer] grocery_ingredient_master_seed.json 없음, ingredient_master 시드 생략");
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            List<IngredientMasterSeedRow> rows = objectMapper.readValue(in, new TypeReference<List<IngredientMasterSeedRow>>() {});
            int inserted = 0;
            for (IngredientMasterSeedRow row : rows) {
                String normalized = row.normalizedName().trim();
                if (normalized.length() > 100) {
                    normalized = normalized.substring(0, 100);
                }
                if (ingredientMasterRepository.findByNormalizedNameIgnoreCase(normalized).isPresent()) {
                    continue;
                }
                Integer categoryId = ingredientCategoryRepository.findByCategoryCode(row.categoryCode())
                        .map(IngredientCategory::getCategoryId)
                        .map(Long::intValue)
                        .orElse(null);
                if (categoryId == null) {
                    log.warn("[DevDataInitializer] 알 수 없는 categoryCode={}, 행 건너뜀 normalizedName={}",
                            row.categoryCode(), normalized);
                    continue;
                }
                String ko = row.aliasesKo() == null || row.aliasesKo().isEmpty()
                        ? ""
                        : String.join(",", row.aliasesKo());
                String aliasText = "ko:" + ko
                        + "|src:mapping_grocery_dataset"
                        + "|key:" + row.sourceKey()
                        + "|type:" + row.sourceType();
                IngredientMaster m = new IngredientMaster();
                setField(m, "canonicalName", normalized);
                setField(m, "normalizedName", normalized);
                setField(m, "categoryId", categoryId);
                setField(m, "aliasText", aliasText);
                setField(m, "isActive", true);
                setField(m, "createdAt", LocalDateTime.now());
                ingredientMasterRepository.save(m);
                inserted++;
            }
            log.info("[DevDataInitializer] ingredient_master (규격 시드 grocery) {} 건 신규 삽입", inserted);
        }
    }

    private static IngredientCategory category(String code, String name, boolean active, int sortOrder) {
        IngredientCategory c = new IngredientCategory();
        c.setCategoryCode(code);
        c.setCategoryName(name);
        c.setIsActive(active);
        c.setSortOrder(sortOrder);
        return c;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IngredientMasterSeedRow(
            String normalizedName,
            String categoryCode,
            List<String> aliasesKo,
            String sourceKey,
            String sourceType
    ) {}
}
