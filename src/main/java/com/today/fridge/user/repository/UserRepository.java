package com.today.fridge.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
