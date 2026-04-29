package com.today.fridge.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "allergen_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllergenGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name="is_active",nullable = false)
    private Boolean isActive = true;
    
    public static AllergenGroup create(String code, String name, String description) {
        AllergenGroup group = new AllergenGroup();
        group.code = code;
        group.name = name;
        group.description = description;
        group.isActive = true;
        return group;
    }
}