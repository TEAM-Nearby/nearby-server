// Apple Refresh Token을 저장 전 암호화하고 조회 시 복호화하는 구성요소
package com.sopt.nearby.user.adapter.out.persistence;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppleRefreshTokenCipher {

	private static final int NONCE_LENGTH = 12;
	private static final int TAG_BITS = 128;
	private final SecretKeySpec key;
	private final SecureRandom random = new SecureRandom();

	public AppleRefreshTokenCipher(@Value("${apple.refresh-token-encryption-key:}") final String encodedKey) {
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(encodedKey);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Apple Refresh Token 암호화 키가 유효하지 않습니다.", exception);
		}
		if (decoded.length != 32) {
			throw new IllegalStateException("Apple Refresh Token 암호화 키는 Base64 인코딩된 32바이트여야 합니다.");
		}
		this.key = new SecretKeySpec(decoded, "AES");
	}

	public String encrypt(final String plaintext) {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
			byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			return "v1:" + Base64.getEncoder().encodeToString(nonce) + ":"
					+ Base64.getEncoder().encodeToString(encrypted);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Apple Refresh Token 암호화에 실패했습니다.", exception);
		}
	}

	public String decrypt(final String stored) {
		String[] parts = stored.split(":", 3);
		if (parts.length != 3 || !"v1".equals(parts[0])) {
			throw new IllegalStateException("Apple Refresh Token 저장 형식이 유효하지 않습니다.");
		}
		try {
			byte[] nonce = Base64.getDecoder().decode(parts[1]);
			if (nonce.length != NONCE_LENGTH) {
				throw new IllegalStateException("Apple Refresh Token nonce가 유효하지 않습니다.");
			}
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
			return new String(cipher.doFinal(Base64.getDecoder().decode(parts[2])), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new IllegalStateException("Apple Refresh Token 복호화에 실패했습니다.", exception);
		}
	}
}
