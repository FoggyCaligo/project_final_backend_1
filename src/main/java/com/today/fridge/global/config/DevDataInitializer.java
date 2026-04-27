package com.today.fridge.global.config;

import com.today.fridge.ingredient.entity.IngredientCategory;
import com.today.fridge.ingredient.repository.IngredientCategoryRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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

    public DevDataInitializer(UserRepository userRepository,
                               IngredientCategoryRepository ingredientCategoryRepository) {
        this.userRepository = userRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUser();
        seedCategories();
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
}
