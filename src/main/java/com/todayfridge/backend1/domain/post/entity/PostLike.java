package com.todayfridge.backend1.domain.post.entity;

import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "post_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_like_post_user", columnNames = {"post_id", "user_id"})
})
public class PostLike extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "post_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    protected PostLike() {}
    public PostLike(Post post, User user) { this.post = post; this.user = user; }
    public Post getPost() { return post; }
}
