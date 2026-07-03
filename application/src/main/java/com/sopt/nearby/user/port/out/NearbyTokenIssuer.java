// Nearby 자체 액세스 토큰과 리프레시 토큰 발급을 위임하는 포트
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.application.IssuedNearbyTokens;
import com.sopt.nearby.user.application.TokenIssueRequest;

public interface NearbyTokenIssuer {

	IssuedNearbyTokens issue(TokenIssueRequest request);
}
