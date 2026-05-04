package com.today.fridge.post.service;

import com.today.fridge.post.entity.Post;
import com.today.fridge.post.entity.PostLike;
import com.today.fridge.post.repository.PostLikeRepository;
import com.today.fridge.post.repository.PostRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addLike(Long postId, Long userId) {
        if (!postLikeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(user);
            postLikeRepository.save(postLike);

            long currentCount = post.getLikeCount() == null ? 0L : post.getLikeCount();
            post.setLikeCount(currentCount + 1);
        }
    }

    @Transactional
    public void removeLike(Long postId, Long userId) {
        if (postLikeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            postLikeRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
            
            long currentCount = post.getLikeCount() == null ? 0L : post.getLikeCount();
            post.setLikeCount(Math.max(0, currentCount - 1));
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLikeStatusAndCount(Long postId, Long userId) {
        boolean isLiked = postLikeRepository.existsByPost_PostIdAndUser_UserId(postId, userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
                
        long likeCount = post.getLikeCount() == null ? 0L : post.getLikeCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);
        return result;
    }
}