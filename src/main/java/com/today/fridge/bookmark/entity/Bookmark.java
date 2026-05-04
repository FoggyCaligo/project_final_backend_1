package com.today.fridge.bookmark.entity;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // 💡 Setter 추가

import java.time.LocalDateTime;

@Getter
@Setter // 💡 북마크 저장을 위해 추가
@NoArgsConstructor
@Entity
@Table(name = "bookmark")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}