// 혼밥 장소 즐겨찾기 중복 저장 충돌을 나타내는 도메인 예외
package com.sopt.nearby.place.domain.exception;

public class DuplicateSoloDiningFavoriteException extends RuntimeException {

    public DuplicateSoloDiningFavoriteException(final Throwable cause) {
        super(cause);
    }
}
