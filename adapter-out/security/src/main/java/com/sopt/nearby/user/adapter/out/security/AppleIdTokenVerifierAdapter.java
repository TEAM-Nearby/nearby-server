// 애플 OIDC ID 토큰을 검증해 애플 사용자 식별자를 반환하는 어댑터
package com.sopt.nearby.user.adapter.out.security;


import com.sopt.nearby.user.application.VerifiedUser;
import com.sopt.nearby.user.exception.AppleLoginFailedException;
import com.sopt.nearby.user.port.out.AppleIdTokenVerifier;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class AppleIdTokenVerifierAdapter implements AppleIdTokenVerifier {

    private final JwtDecoder jwtDecoder;
    private final String clientId;

    public AppleIdTokenVerifierAdapter(
            @Qualifier("appleJwtDecoder") final JwtDecoder jwtDecoder,
            @Value("${apple.client-id:${APPLE_CLIENT_ID:}}") final String clientId
    ) {
        this.jwtDecoder = jwtDecoder;
        this.clientId = clientId;
    }

    @Override
    public VerifiedUser verify(final String idToken, final String nonce) {
        if (isBlank(clientId)) {
            throw new AppleLoginFailedException();
        }

        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            validateAudience(jwt.getAudience());
            validateNonce(jwt.getClaimAsString("nonce"), nonce);
            validateSubject(jwt.getSubject());
            return new VerifiedUser(jwt.getSubject());
        } catch (JwtException exception) {
            throw new AppleLoginFailedException();
        }
    }

    private void validateAudience(final List<String> audience) {
        if (audience == null || !audience.contains(clientId)) {
            throw new AppleLoginFailedException();
        }
    }

    private void validateNonce(final String tokenNonce, final String expectedNonce) {
        if (isBlank(expectedNonce) || !expectedNonce.equals(tokenNonce)) {
            throw new AppleLoginFailedException();
        }
    }

    private void validateSubject(final String subject) {
        if (isBlank(subject)) {
            throw new AppleLoginFailedException();
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}