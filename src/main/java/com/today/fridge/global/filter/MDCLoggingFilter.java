package com.today.fridge.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/*
 * HTTP 요청별로 고유한 Trace ID(requestId)를 부여하는 MDC(Mapped Diagnostic Context) 필터입니다.
 * 
 * 주요기능
 * - 모든 로그에 요청별 고유 ID를 삽입하여 분산 환경 및 멀티스레드 환경에서 추적을 용이하게 함
 * - 외부 시스템에서 전달받은 'X-Request-ID'가 있다면 이를 사용하고, 없으면 새로 생성
 * - 응답 헤더에도 동일한 ID를 포함하여 클라이언트가 에러 발생 시 추적 ID를 제공할 수 있도록 함
 * 
 * 사용방법
 * - 일반 log.info(), log.error() 등 모든 SLF4j 로깅 메소드는 자동으로 MDCContext를 포함합니다.
 * - 그 이외 MDC.put("requestId","value")를 사용하여 MDCContext에 값을 추가할 수 있습니다.
 * - 1회성으로 사용할 값은 finally 블록에서 MDC.remove("key")를 사용하여 제거할 것을 권장합니다.
 * 
 * 장점
 * - 각 요청 체인 별 ID가 붙어 어디에서 어떻게 문제가 발생하였는지 확인하기 좋습니다.
 * - 디버깅 시 어떤 요청에 대한 로그인지 쉽게 파악할 수 있습니다.
 * 
 * 사용 예시
 * - log.info("Request ID: {}", requestId);
 * 
 * try {
 *      MDC.put("userId", userId); // 임시적으로 컨텍스트에 값을 추가할 수 있습니다.
 *      log.info("Processing order #{}", orderId);
 * } finally {
 *      MDC.remove("userId"); // 임시적으로 추가한 값을 제거합니다.
 * }
 */

@Component
public class MDCLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 요청 헤더에서 ID 추출 및 없을 경우 신규 생성
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }

            // 2. MDC 컨텍스트에 ID 저장 (이후 해당 스레드의 모든 로그에 자동 포함될 준비 완료)
            MDC.put(MDC_REQUEST_ID_KEY, requestId);

            // 3. 응답 헤더에 ID 포함 (클라이언트가 에러 발생 시 추적 ID 제공 가능)
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // 4. 필터 체인 실행 (실제 컨트롤러/서비스 로직 호출)
            filterChain.doFilter(request, response);
        } finally {
            // 5. 메모리 누수를 방지하기 위해 finally 블록에서 MDC를 항상 비웁니다.
            MDC.clear();
        }
    }

}