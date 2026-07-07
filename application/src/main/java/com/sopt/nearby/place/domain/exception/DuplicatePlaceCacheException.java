// 장소 캐시 중복 저장 충돌을 나타내는 도메인 예외
package com.sopt.nearby.place.domain.exception;

public class DuplicatePlaceCacheException extends RuntimeException {

    public DuplicatePlaceCacheException(final Throwable cause) {
        super(cause);
    }
}
