package com.today.fridge.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.today.fridge.global.external.fastapi.FastApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "FastApiTest", description = "FastApiTestController API")
public class FastApiTestController {

    private final FastApiService fastApiService;

    public FastApiTestController(FastApiService fastApiService) {
        this.fastApiService = fastApiService;
    }

    @GetMapping("/api/fastapi-test")
    @Operation(summary = "FastApiTest API")
    public String fastApiTest() {
        return fastApiService.health();
    }
}