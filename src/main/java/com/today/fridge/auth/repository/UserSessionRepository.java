package com.today.fridge.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.auth.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

}
