package com.today.fridge.global.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.ingredient.entity.IngredientCategory;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.repository.IngredientCategoryRepository;
import com.today.fridge.recommendation.entity.AllergenGroup;
import com.today.fridge.recommendation.entity.AllergenIngredientMap;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.repository.AllergenGroupRepository;
import com.today.fridge.recommendation.repository.AllergenIngredientMapRepository;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    private final ConditionCodeRepository conditionCodeRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final ObjectMapper objectMapper;
    private final AllergenGroupRepository allergenGroupRepository;
    private final AllergenIngredientMapRepository allergenIngredientMapRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedGroceryIngredientMaster;

    public DevDataInitializer(UserRepository userRepository,
                              IngredientCategoryRepository ingredientCategoryRepository,
                              IngredientMasterRepository ingredientMasterRepository,
                              ObjectMapper objectMapper,
                              ConditionCodeRepository conditionCodeRepository,
                              AllergenGroupRepository allergenGroupRepository,
                              AllergenIngredientMapRepository allergenIngredientMapRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.dev.seed-grocery-ingredient-master:false}") boolean seedGroceryIngredientMaster) {
        this.userRepository = userRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.ingredientMasterRepository = ingredientMasterRepository;
        this.objectMapper = objectMapper;
        this.conditionCodeRepository = conditionCodeRepository;
        this.allergenGroupRepository = allergenGroupRepository;
        this.allergenIngredientMapRepository = allergenIngredientMapRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedGroceryIngredientMaster = seedGroceryIngredientMaster;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUser();
        seedCategories();
        seedConditionCodes();
        seedAllergenGroups();
        if (seedGroceryIngredientMaster) {
            seedIngredientMasterFromCanonicalGroceryFile();
        } else {
            log.info("[DevDataInitializer] ingredient_master grocery JSON 시드 생략 (app.dev.seed-grocery-ingredient-master=false)");
        }
    }

    // testuser 계정 비밀번호: Test@1234
    private void seedUser() throws Exception {
        if (userRepository.count() == 0) {
            User user = new User();
            setField(user, "loginId", "testuser");
            setField(user, "email", "testuser@test.com");
            setField(user, "passwordHash", passwordEncoder.encode("Test@1234"));
            setField(user, "nickname", "테스트유저");
            setField(user, "status", "ACTIVE");
            setField(user, "emailVerified", true);
            setField(user, "createdAt", OffsetDateTime.now());
            setField(user, "updatedAt", OffsetDateTime.now());
            userRepository.save(user);
            log.info("[DevDataInitializer] 테스트 유저 생성 완료 (loginId=testuser, password=Test@1234)");
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
     * 동일 {@code normalized_name} 또는 {@code canonical_name}(시드에서는 동일 문자열)이 이미 있으면 건너뜁니다.
     * DB에만 canonical이 겹치는 행이 있어도 유니크 제약으로 기동이 실패하지 않도록 합니다.
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
                if (ingredientMasterRepository.findByNormalizedNameIgnoreCase(normalized).isPresent()
                        || ingredientMasterRepository.findByCanonicalNameIgnoreCase(normalized).isPresent()) {
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
    private static ConditionCode condition(
            String group,
            String code,
            String name,
            String description
    ) {
        return ConditionCode.create(group, code, name, description);
    }
    private void seedConditionCodes() {
        if (conditionCodeRepository.count() > 0) {
            log.info("[DevDataInitializer] condition_code 데이터 존재, 시드 생략");
            return;
        }

        List<ConditionCode> conditions = List.of(
                condition("DIET", "DIET_LOW_CALORIE", "다이어트/저칼로리", "저칼로리, 저지방, 채소 중심, 두부, 버섯, 닭가슴살 등 가벼운 식단에 적합한 레시피"),
                condition("HEALTH", "LOW_SODIUM", "저염식", "짜지 않고 나트륨 부담이 적으며 소금, 간장, 된장 사용이 적은 담백한 레시피"),
                condition("ALLERGY", "ALLERGY_EGG", "계란 알러지", "계란, 달걀, 마요네즈, 계란물, 지단 등 계란 성분이 포함된 레시피"),
                condition("ALLERGY", "ALLERGY_MILK", "우유 알러지", "우유, 치즈, 버터, 생크림, 요거트 등 유제품 성분이 포함된 레시피")
        );

        conditionCodeRepository.saveAll(conditions);
        log.info("[DevDataInitializer] condition_code 시드 데이터 {} 건 삽입 완료", conditions.size());
    }
    private AllergenGroup allergen(
    	    String code,
    	    String name,
    	    String desc
    	){
    	    return AllergenGroup.create(
    	        code,
    	        name,
    	        desc
    	    );
    	}
    private void seedAllergenGroups() {

        if (allergenGroupRepository.count() > 0) {
            log.info("[DevDataInitializer] allergen 데이터 존재, 시드 생략");
            return;
        }

        AllergenGroup soy =
                allergenGroupRepository.save(
                    allergen("SOY","대두","대두 유발 물질")
                );

        AllergenGroup wheat =
                allergenGroupRepository.save(
                    allergen("WHEAT","밀","밀 유발 물질")
                );

        AllergenGroup egg =
                allergenGroupRepository.save(
                    allergen("EGG","난류","계란 유발 물질")
                );

        saveAllergenIngredients(
            soy,
            List.of("콩","대두","두부","간장","된장","고추장","유부")
        );

        saveAllergenIngredients(
            wheat,
            List.of("밀","밀가루","면","국수","빵","부침가루")
        );

        saveAllergenIngredients(
            egg,
            List.of("계란","달걀","난백","난황")
        );

        log.info("[DevDataInitializer] allergen seed 완료");
    }
    private void saveAllergenIngredients(
            AllergenGroup group,
            List<String> ingredients
    ){
        List<AllergenIngredientMap> maps =
            ingredients.stream()
                .map(i -> AllergenIngredientMap.create(group, i))
                .toList();

        allergenIngredientMapRepository.saveAll(maps);
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
