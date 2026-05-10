package com.today.fridge.substitution.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.substitution.dto.SubstitutionSuggestRequest;
import com.today.fridge.substitution.dto.SubstitutionSuggestResponse;
import com.today.fridge.substitution.service.SubstitutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/substitutions")
public class SubstitutionController {

    private final SubstitutionService substitutionService;

    @PostMapping("/suggest")
    public ResponseEntity<ApiResponse<SubstitutionSuggestResponse>> suggest(
            @RequestBody SubstitutionSuggestRequest request
    ) {
        SubstitutionSuggestResponse response =
                substitutionService.suggest(request);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}