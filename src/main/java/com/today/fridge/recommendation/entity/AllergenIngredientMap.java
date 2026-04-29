package com.today.fridge.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "allergen_ingredient_map")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllergenIngredientMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergen_group_id", nullable = false)
    private AllergenGroup allergenGroup;

    @Column(nullable = false, length = 100)
    private String ingredientName;

    @Column(name="is_active",nullable = false)
    private Boolean isActive = true;
    
    public static AllergenIngredientMap create(AllergenGroup group, String ingredientName) {
        AllergenIngredientMap map = new AllergenIngredientMap();
        map.allergenGroup = group;
        map.ingredientName = ingredientName;
        map.isActive = true;
        return map;
    }
}
