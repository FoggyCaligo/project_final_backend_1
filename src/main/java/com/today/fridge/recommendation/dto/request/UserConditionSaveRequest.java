package com.today.fridge.recommendation.dto.request;


import lombok.Getter;

@Getter
public class UserConditionSaveRequest {

    private Boolean milkAllergy;
    private Boolean eggAllergy;
    private Boolean diet;
    private Boolean lowSodium;
}