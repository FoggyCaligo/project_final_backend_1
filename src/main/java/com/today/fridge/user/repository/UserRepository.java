package com.today.fridge.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<User> findByKakaoId(Long kakaoId);
}
