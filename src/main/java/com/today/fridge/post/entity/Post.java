package com.today.fridge.post.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User authorUser;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "report_count")
    private Long reportCount;

    @Column(name = "report_count")
    private Long reportCount;

    @Column(name = "report_count")
    private Long reportCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}