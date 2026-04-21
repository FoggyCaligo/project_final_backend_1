package com.today.fridge.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.user.entity.UserFollow;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

}
