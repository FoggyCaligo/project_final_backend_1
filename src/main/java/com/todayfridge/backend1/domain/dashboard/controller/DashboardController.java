package com.todayfridge.backend1.domain.dashboard.controller;

import com.todayfridge.backend1.domain.dashboard.service.DashboardService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(dashboardService.home(loginUser.getUserId()));
    }
}
