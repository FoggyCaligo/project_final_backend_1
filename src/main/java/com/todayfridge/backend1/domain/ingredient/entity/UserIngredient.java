package com.todayfridge.backend1.domain.ingredient.entity;

import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_ingredients")
public class UserIngredient extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    @Column(nullable = false, length = 120)
    private String ingredientName;
    @Column(nullable = false, length = 120)
    private String normalizedName;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(length = 50)
    private String quantity;
    @Column(length = 30)
    private String unit;
    private LocalDate expirationDate;
    @Column(length = 30)
    private String storageType;

    protected UserIngredient() {}
    public UserIngredient(User user, String ingredientName, String normalizedName, String category,
                          String quantity, String unit, LocalDate expirationDate, String storageType) {
        this.user = user;
        this.ingredientName = ingredientName;
        this.normalizedName = normalizedName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.expirationDate = expirationDate;
        this.storageType = storageType;
    }

    public Long getId() { return id; }
    public String getIngredientName() { return ingredientName; }
    public String getNormalizedName() { return normalizedName; }
    public String getCategory() { return category; }
    public String getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getStorageType() { return storageType; }

    public void update(String ingredientName, String normalizedName, String category,
                       String quantity, String unit, LocalDate expirationDate, String storageType) {
        this.ingredientName = ingredientName;
        this.normalizedName = normalizedName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.expirationDate = expirationDate;
        this.storageType = storageType;
    }
}
