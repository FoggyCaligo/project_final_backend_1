package com.today.fridge.post.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
// 💡 DB 테이블은 DDL에 맞춰 "user_follow"로 유지하면서 클래스명만 통일합니다.
@Table(name = "user_follow") 
@Getter
@Setter
@NoArgsConstructor
public class PostFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    // 팔로우를 하는 사람 (나)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_user_id", nullable = false)
    private User followerUser;

    // 팔로우를 받는 사람 (상대방)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_user_id", nullable = false)
    private User followeeUser;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}