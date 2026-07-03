// ApiExceptions 애너테이션을 OpenAPI 예외 응답 예시로 변환하는 커스터마이저
package com.sopt.nearby.shared.adapter.in.web.swagger;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;
import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class ApiExceptionsOperationCustomizer implements OperationCustomizer {

	private static final String MEDIA_TYPE_JSON = "application/json";

	@Override
	public Operation customize(final Operation operation, final HandlerMethod handlerMethod) {
		ApiExceptions apiExceptions = findApiExceptions(handlerMethod);
		if (apiExceptions == null) {
			return operation;
		}

		if (operation.getResponses() == null) {
			operation.setResponses(new ApiResponses());
		}

		Map<HttpStatus, Map<String, Example>> groupedExamples = new LinkedHashMap<>();
		for (Class<? extends BusinessException> exceptionClass : apiExceptions.value()) {
			ErrorCode errorCode = resolveErrorCode(exceptionClass);
			HttpStatus status = resolveHttpStatus(exceptionClass, errorCode);
			Example example = new Example()
					.summary(errorCode.name())
					.description(errorCode.message())
					.value(CommonResponse.error(status.value(), errorCode));

			groupedExamples
					.computeIfAbsent(status, key -> new LinkedHashMap<>())
					.put(errorCode.name(), example);
		}

		groupedExamples.forEach((status, examples) -> mergeIntoResponses(operation.getResponses(), status, examples));
		return operation;
	}

	private ApiExceptions findApiExceptions(final HandlerMethod handlerMethod) {
		ApiExceptions apiExceptions = handlerMethod.getMethodAnnotation(ApiExceptions.class);
		if (apiExceptions != null) {
			return apiExceptions;
		}

		Method method = handlerMethod.getMethod();
		for (Class<?> interfaceType : handlerMethod.getBeanType().getInterfaces()) {
			apiExceptions = findApiExceptionsOnInterface(interfaceType, method);
			if (apiExceptions != null) {
				return apiExceptions;
			}
		}

		return null;
	}

	private ApiExceptions findApiExceptionsOnInterface(final Class<?> interfaceType, final Method sourceMethod) {
		for (Method interfaceMethod : interfaceType.getMethods()) {
			if (hasSameSignature(interfaceMethod, sourceMethod)) {
				return AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, ApiExceptions.class);
			}
		}

		return null;
	}

	private boolean hasSameSignature(final Method candidate, final Method source) {
		return candidate.getName().equals(source.getName())
				&& Arrays.equals(candidate.getParameterTypes(), source.getParameterTypes());
	}

	private void mergeIntoResponses(
			final ApiResponses responses,
			final HttpStatus status,
			final Map<String, Example> examples
	) {
		String statusKey = String.valueOf(status.value());
		ApiResponse apiResponse = responses.get(statusKey);
		if (apiResponse == null) {
			apiResponse = new ApiResponse().description(status.getReasonPhrase());
			responses.addApiResponse(statusKey, apiResponse);
		}

		Content content = apiResponse.getContent();
		if (content == null) {
			content = new Content();
			apiResponse.setContent(content);
		}

		MediaType mediaType = content.get(MEDIA_TYPE_JSON);
		if (mediaType == null) {
			mediaType = new MediaType();
			content.addMediaType(MEDIA_TYPE_JSON, mediaType);
		}

		Map<String, Example> mergedExamples = mediaType.getExamples();
		if (mergedExamples == null) {
			mergedExamples = new LinkedHashMap<>();
			mediaType.setExamples(mergedExamples);
		}
		mergedExamples.putAll(examples);
	}

	private ErrorCode resolveErrorCode(final Class<? extends BusinessException> exceptionClass) {
		try {
			Constructor<? extends BusinessException> constructor = exceptionClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance().getErrorCode();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(
					"@ApiExceptions에 등록된 " + exceptionClass.getName()
							+ " 클래스는 ErrorCode를 주입하는 no-args 생성자를 가져야 합니다.",
					exception
			);
		}
	}

	private HttpStatus resolveHttpStatus(
			final Class<? extends BusinessException> exceptionClass,
			final ErrorCode errorCode
	) {
		if (NotFoundException.class.isAssignableFrom(exceptionClass)) {
			return HttpStatus.NOT_FOUND;
		}
		if (errorCode.name().startsWith("FORBIDDEN")) {
			return HttpStatus.FORBIDDEN;
		}
		if (errorCode.name().equals("UNAUTHORIZED")) {
			return HttpStatus.UNAUTHORIZED;
		}

		return HttpStatus.BAD_REQUEST;
	}
}
