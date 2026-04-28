package com.today.fridge.auth.repository;

import com.today.fridge.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
}
