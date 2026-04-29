package com.today.fridge.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationStore verificationStore;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * 인증 코드를 생성하고 해당 이메일로 발송한다.
     * @param email 수신 이메일 주소
     */
    public void sendVerificationCode(String email) {
        String code = verificationStore.generateCode(email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[오늘냉장고] 이메일 인증 코드");
        message.setText(
                "안녕하세요! 오늘냉장고입니다.\n\n" +
                "이메일 인증 코드: " + code + "\n\n" +
                "인증 코드는 5분간 유효합니다.\n" +
                "본인이 요청하지 않은 경우 이 메일을 무시해 주세요."
        );
        mailSender.send(message);
    }

    /**
     * 인증 코드를 검증한다.
     * @return true: 인증 성공
     * @throws IllegalArgumentException 코드가 일치하지 않거나 만료된 경우
     */
    public void verifyCode(String email, String code) {
        if (!verificationStore.verify(email, code)) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다");
        }
    }
}
