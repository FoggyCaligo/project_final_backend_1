package com.today.fridge.post.service;

import com.today.fridge.post.entity.PostFollow;
import com.today.fridge.post.repository.PostFollowRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostFollowService {

    private final PostFollowRepository postFollowRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addFollow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        if (!postFollowRepository.existsByFollowerUser_UserIdAndFolloweeUser_UserId(followerId, followeeId)) {
            User follower = userRepository.findById(followerId)
                    .orElseThrow(() -> new IllegalArgumentException("팔로워를 찾을 수 없습니다."));
            User followee = userRepository.findById(followeeId)
                    .orElseThrow(() -> new IllegalArgumentException("팔로우할 대상을 찾을 수 없습니다."));

            PostFollow follow = new PostFollow();
            follow.setFollowerUser(follower);
            follow.setFolloweeUser(followee);
            postFollowRepository.save(follow);
        }
    }

    @Transactional
    public void removeFollow(Long followerId, Long followeeId) {
        if (postFollowRepository.existsByFollowerUser_UserIdAndFolloweeUser_UserId(followerId, followeeId)) {
            postFollowRepository.deleteByFollowerUser_UserIdAndFolloweeUser_UserId(followerId, followeeId);
        }
    }

    @Transactional(readOnly = true)
    public boolean checkFollowStatus(Long followerId, Long followeeId) {
        return postFollowRepository.existsByFollowerUser_UserIdAndFolloweeUser_UserId(followerId, followeeId);
    }
}