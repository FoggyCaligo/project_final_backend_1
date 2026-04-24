package com.today.fridge.global.controller;

import com.today.fridge.global.filter.RequestIdFilter;
import com.today.fridge.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, String>>> root(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                        "service", "today-fridge-api",
                        "hint", "API는 /api/v1 이하를 사용하세요."),
                requestId));
    }

    /** 브라우저 기본 favicon 요청으로 인한 불필요한 404/500 로그 방지 */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
