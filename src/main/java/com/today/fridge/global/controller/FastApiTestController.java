package com.today.fridge.global.controller;

import com.today.fridge.global.external.fastapi.FastApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FastApiTestController {

    private final FastApiService fastApiService;

    public FastApiTestController(FastApiService fastApiService) {
        this.fastApiService = fastApiService;
    }

    @GetMapping("/api/fastapi-test")
    public String fastApiTest() {
        return fastApiService.health();
    }
}