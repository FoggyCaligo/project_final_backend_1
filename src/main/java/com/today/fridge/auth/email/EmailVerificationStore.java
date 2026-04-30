package com.today.fridge.auth.email;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증 코드를 메모리에서 관리한다.
 * 서버 재시작 시 초기화되므로, 발급된 코드는 모두 무효화된다.
 */
@Component
public class EmailVerificationStore {

    private static final int EXPIRY_MINUTES = 5;
    private static final int CODE_BOUND = 1_000_000;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** 6자리 인증 코드를 생성하고 저장한 뒤 반환한다. */
    public String generateCode(String email) {
        String code = String.format("%06d", random.nextInt(CODE_BOUND));
        store.put(email, new Entry(code, LocalDateTime.now().plusMinutes(EXPIRY_MINUTES)));
        return code;
    }

    /**
     * 코드가 유효한지 확인하고, 일치하면 저장소에서 제거한다.
     * @return true: 인증 성공 / false: 코드 불일치 또는 만료
     */
    public boolean verify(String email, String code) {
        Entry entry = store.get(email);
        if (entry == null || LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email);
            return false;
        }
        if (!entry.code().equals(code)) {
            return false;
        }
        store.remove(email);
        return true;
    }

    private record Entry(String code, LocalDateTime expiresAt) {}
}
