package com.today.fridge.global.external;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@todayfridge.com}")
    private String fromAddress;

    @Value("${app.mail.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 회원가입 이메일 인증 메일 발송 (비동기)
     *
     * @param toEmail 수신자 이메일
     * @param token   인증 토큰 (UUID)
     */
    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String subject = "[오늘냉장고] 이메일 인증을 완료해주세요";
        String body = buildVerificationEmailBody(verifyUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // HTML 형식

            mailSender.send(message);
            log.info("인증 이메일 발송 완료: {}", toEmail);
        } catch (MessagingException e) {
            log.error("인증 이메일 발송 실패: {}", toEmail, e);
            throw new ExceptionTemplate(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * 이메일 인증 HTML 본문 생성
     */
    private String buildVerificationEmailBody(String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; padding: 40px; background: #f5f5f5;">
              <div style="max-width: 480px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
                <h2 style="color: #2d3436; margin-bottom: 16px;">🥗 오늘냉장고 이메일 인증</h2>
                <p style="color: #636e72; line-height: 1.6;">
                  안녕하세요! 오늘냉장고에 가입해 주셔서 감사합니다.<br/>
                  아래 버튼을 클릭하여 이메일 인증을 완료해주세요.
                </p>
                <div style="text-align: center; margin: 32px 0;">
                  <a href="%s"
                     style="display: inline-block; padding: 14px 32px; background: #00b894;
                            color: #fff; text-decoration: none; border-radius: 8px;
                            font-size: 16px; font-weight: bold;">
                    이메일 인증하기
                  </a>
                </div>
                <p style="color: #b2bec3; font-size: 12px;">
                  이 링크는 24시간 동안 유효합니다.<br/>
                  본인이 요청하지 않았다면 이 메일을 무시해주세요.
                </p>
              </div>
            </body>
            </html>
            """.formatted(verifyUrl);
    }
}
