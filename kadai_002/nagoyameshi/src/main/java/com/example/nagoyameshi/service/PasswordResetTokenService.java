package com.example.nagoyameshi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.PasswordResetToken;
import com.example.nagoyameshi.entity.User;
import com.example.nagoyameshi.repository.PasswordResetTokenRepository;
import com.example.nagoyameshi.repository.UserRepository;

@Service
public class PasswordResetTokenService {
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final UserRepository userRepository;

	public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository,
			UserRepository userRepository) {
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.userRepository = userRepository;
	}

	public PasswordResetToken findTokenByEmail(String email) {
		return passwordResetTokenRepository.findByUserEmail(email);
	}

	@Transactional
	public void createOrUpdateToken(String email, String token) {
		// メールアドレスで既存のトークンを検索
		PasswordResetToken existingToken = passwordResetTokenRepository.findByUserEmail(email);

		if (existingToken != null && existingToken.getToken() != null && !existingToken.getToken().isEmpty()) {
			// 既存のトークンがあれば削除

			passwordResetTokenRepository.delete(existingToken);
			passwordResetTokenRepository.flush();
		}

		// 新しいトークンを作成
		PasswordResetToken passwordResetToken = new PasswordResetToken();
		passwordResetToken.setToken(token);

		// メールアドレスからユーザーを取得
		User user = userRepository.findByEmail(email);
		passwordResetToken.setUser(user);

		// 新しいトークンを保存
		passwordResetTokenRepository.save(passwordResetToken);
	}

	@Transactional
	public void deleteTokenByEmail(String email) {
		passwordResetTokenRepository.deleteByUserEmail(email);
	}

	public PasswordResetToken findTokenByToken(String token) {
		return passwordResetTokenRepository.findByToken(token);
	}

}
