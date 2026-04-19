package com.todayfridge.backend1.domain.post.entity;

import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "author_user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 4000)
    private String content;
    @Column(nullable = false)
    private long likeCount = 0L;
    @Column(nullable = false)
    private long viewCount = 0L;

    protected Post() {}
    public Post(User author, String title, String content) { this.author = author; this.title = title; this.content = content; }
    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getLikeCount() { return likeCount; }
    public long getViewCount() { return viewCount; }
    public void update(String title, String content) { this.title = title; this.content = content; }
    public void increaseLikeCount() { this.likeCount += 1; }
    public void decreaseLikeCount() { if (this.likeCount > 0) this.likeCount -= 1; }
    public void increaseViewCount() { this.viewCount += 1; }
}
