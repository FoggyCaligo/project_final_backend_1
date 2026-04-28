package com.today.fridge.recommendation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "condition_code")
public class ConditionCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    private Long conditionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_group", nullable = false, length = 30)
    private ConditionGroup conditionGroup;

    @Column(name = "condition_code", nullable = false, unique = true, length = 50)
    private String conditionCode;

    @Column(name = "condition_name", nullable = false, length = 50)
    private String conditionName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}