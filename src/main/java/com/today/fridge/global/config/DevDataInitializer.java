package com.today.fridge.global.config;

import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * 개발(local) 환경에서 테스트용 유저가 없을 경우 자동으로 1건 생성합니다.
 * X-User-Id: 1 헤더로 API를 테스트할 때 사용됩니다.
 * 운영(prod) 환경에서는 동작하지 않습니다.
 */
@Component
@Profile("local")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;

    public DevDataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
