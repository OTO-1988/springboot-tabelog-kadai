package com.example.nagoyameshi.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.PasswordResetToken;
import com.example.nagoyameshi.entity.User;
import com.example.nagoyameshi.service.PasswordResetTokenService;
import com.example.nagoyameshi.service.UserService;

@Controller
@RequestMapping("/password-reset")
public class PasswordResetController {
	private final UserService userService;
	private final PasswordResetTokenService passwordResetTokenService;
	private final JavaMailSender mailSender;

	public PasswordResetController(UserService userService, PasswordResetTokenService passwordResetTokenService,
			JavaMailSender mailSender) {
		this.userService = userService;
		this.passwordResetTokenService = passwordResetTokenService;
		this.mailSender = mailSender;
	}

	// パスワードリセットリクエストページ表示
	@GetMapping("/request")
	public String showResetRequestPage() {
		return "password-reset/request";
	}

	// パスワードリセットリンク送信処理
	@PostMapping("/request")
	public String sendResetLink(@RequestParam("email") String email, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		try {
			// ユーザーが登録されているか確認
			User user = userService.findUserByEmail(email);
			if (user == null) {
				redirectAttributes.addFlashAttribute("errorMessage", "そのメールアドレスは登録されていません。");
				return "redirect:/password-reset/request";
			}

			// トークンを生成し、既存のトークンがあれば削除
			String token = UUID.randomUUID().toString();
			passwordResetTokenService.createOrUpdateToken(email, token);

			// リセットURLを作成
			String resetUrl = request.getRequestURL().toString().replace("/request", "/reset") + "/" + token;

			// メール送信
			sendPasswordResetEmail(email, resetUrl);

			redirectAttributes.addFlashAttribute("successMessage", "パスワードリセットリンクが送信されました。");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "エラーが発生しました: " + e.getMessage());
		}
		return "redirect:/password-reset/request";
	}

	private void sendPasswordResetEmail(String email, String resetUrl) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("パスワードリセットのご案内");
		message.setText("以下のリンクをクリックしてパスワードをリセットしてください。\n" + resetUrl);
		mailSender.send(message);
	}

	// パスワードリセットフォーム表示
	@GetMapping("/reset/{token}")
	public String showResetForm(@PathVariable("token") String token, Model model) {
		PasswordResetToken resetToken = passwordResetTokenService.findTokenByToken(token);
		if (resetToken != null) {
			model.addAttribute("token", token);
			return "password-reset/reset";
		} else {
			model.addAttribute("errorToken", "無効なトークンです。");
			return "password-reset/request";
		}
	}

	// パスワード更新処理
	@PostMapping("/reset/{token}")
	public String resetPassword(@PathVariable("token") String token, @RequestParam("password") String password,
			RedirectAttributes redirectAttributes) {
		try {
			PasswordResetToken resetToken = passwordResetTokenService.findTokenByToken(token);
			if (resetToken == null) {
				redirectAttributes.addFlashAttribute("errorToken", "無効なトークンです。");
				return "redirect:/password-reset/request";
			}

			// パスワードを更新
			User user = resetToken.getUser();
			user.setPassword(password); // パスワードエンコードを忘れずに
			userService.updatePassword(user);

			// トークンを削除
			passwordResetTokenService.deleteTokenByEmail(user.getEmail());

			redirectAttributes.addFlashAttribute("successMessage", "パスワードが正常に再設定されました。");
			return "redirect:/login";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "パスワードリセットに失敗しました。");
			return "redirect:/password-reset/request";
		}
	}
}
