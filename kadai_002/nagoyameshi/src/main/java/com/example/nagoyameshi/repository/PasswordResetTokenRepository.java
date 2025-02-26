package com.example.nagoyameshi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.nagoyameshi.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
	PasswordResetToken findByUserEmail(String email); // ユーザーのメールアドレスでトークンを検索

	PasswordResetToken findByToken(String token);

	void deleteByUserEmail(String email);

}