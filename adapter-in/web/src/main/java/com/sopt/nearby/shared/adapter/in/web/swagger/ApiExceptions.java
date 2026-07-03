// Swagger 문서에 비즈니스 예외 응답 예시를 연결하는 애너테이션
package com.sopt.nearby.shared.adapter.in.web.swagger;

import com.sopt.nearby.common.exception.BusinessException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiExceptions {

	Class<? extends BusinessException>[] value();
}
