package com.todayfridge.backend1.domain.bookmark.entity;

import com.todayfridge.backend1.domain.recipe.entity.Recipe;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "bookmarks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookmark_user_recipe", columnNames = {"user_id", "recipe_id"})
})
public class Bookmark extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    @JoinColumn(name = "recipe_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Recipe recipe;

    protected Bookmark() {}
    public Bookmark(User user, Recipe recipe) { this.user = user; this.recipe = recipe; }

    public Long getId() { return id; }
    public Recipe getRecipe() { return recipe; }
}
