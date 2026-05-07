package com.today.fridge.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Test", description = "TestController API")
public class TestController {

    @GetMapping("/api/test")
    @Operation(summary = "Test API")
    public String test() {
        return "ok";
    }
}