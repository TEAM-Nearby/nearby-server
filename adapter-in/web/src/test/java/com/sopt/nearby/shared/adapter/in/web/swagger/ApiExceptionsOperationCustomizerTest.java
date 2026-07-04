// ApiExceptions OpenAPI 커스터마이저의 인터페이스 애너테이션 인식과 예시 병합을 검증하는 테스트
package com.sopt.nearby.shared.adapter.in.web.swagger;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class ApiExceptionsOperationCustomizerTest {

	private static final String APPLICATION_JSON = "application/json";

	private final ApiExceptionsOperationCustomizer customizer = new ApiExceptionsOperationCustomizer();

	@Test
	void readsApiExceptionsFromApiInterfaceAndMergesExamplesByStatusCode() throws NoSuchMethodException {
		Operation operation = new Operation();
		ApiResponses responses = new ApiResponses();
		Example existingExample = new Example().summary("EXISTING_ERROR");
		MediaType badRequestMediaType = new MediaType();
		badRequestMediaType.setExamples(new LinkedHashMap<>());
		badRequestMediaType.getExamples().put("EXISTING_ERROR", existingExample);
		responses.addApiResponse("400", new ApiResponse()
				.description("Bad Request")
				.content(new Content().addMediaType(APPLICATION_JSON, badRequestMediaType)));
		operation.setResponses(responses);
		HandlerMethod handlerMethod = handlerMethod(new TestController(), "get", Long.class);

		customizer.customize(operation, handlerMethod);

		assertThat(jsonExamples(operation, "400"))
				.containsEntry("EXISTING_ERROR", existingExample)
				.containsKeys("INVALID_TEST", "SECOND_INVALID_TEST");
		assertExample(operation, "400", "INVALID_TEST", 400, TestErrorCode.INVALID_TEST);
		assertExample(operation, "400", "SECOND_INVALID_TEST", 400, TestErrorCode.SECOND_INVALID_TEST);
		assertExample(operation, "403", "FORBIDDEN_TEST", 403, TestErrorCode.FORBIDDEN_TEST);
		assertExample(operation, "404", "TEST_NOT_FOUND", 404, TestErrorCode.TEST_NOT_FOUND);
	}

	@Test
	void leavesOperationUntouchedWhenApiExceptionsIsMissing() throws NoSuchMethodException {
		Operation operation = new Operation();
		HandlerMethod handlerMethod = handlerMethod(new TestController(), "plain");

		customizer.customize(operation, handlerMethod);

		assertThat(operation.getResponses()).isNull();
	}

	private HandlerMethod handlerMethod(
			final Object controller,
			final String methodName,
			final Class<?>... parameterTypes
	) throws NoSuchMethodException {
		Method method = controller.getClass().getMethod(methodName, parameterTypes);
		return new HandlerMethod(controller, method);
	}

	private LinkedHashMap<String, Example> jsonExamples(final Operation operation, final String statusCode) {
		return new LinkedHashMap<>(
				operation.getResponses()
						.get(statusCode)
						.getContent()
						.get(APPLICATION_JSON)
						.getExamples()
		);
	}

	private void assertExample(
			final Operation operation,
			final String statusCode,
			final String exampleName,
			final int status,
			final TestErrorCode errorCode
	) {
		Example example = jsonExamples(operation, statusCode).get(exampleName);

		assertThat(example.getSummary()).isEqualTo(errorCode.name());
		assertThat(example.getDescription()).isEqualTo(errorCode.message());
		assertThat(example.getValue()).isEqualTo(CommonResponse.error(status, errorCode));
	}

	private interface TestApi {

		@ApiExceptions({
				InvalidTestException.class,
				SecondInvalidTestException.class,
				ForbiddenTestException.class,
				TestNotFoundException.class
		})
		void get(Long id);
	}

	private static final class TestController implements TestApi {

		@Override
		public void get(final Long id) {
		}

		public void plain() {
		}
	}

	private enum TestErrorCode implements ErrorCode {
		INVALID_TEST("올바르지 않은 테스트 요청입니다."),
		SECOND_INVALID_TEST("두 번째 올바르지 않은 테스트 요청입니다."),
		FORBIDDEN_TEST("테스트 접근 권한이 없습니다."),
		TEST_NOT_FOUND("테스트 대상을 찾을 수 없습니다.");

		private final String message;

		TestErrorCode(final String message) {
			this.message = message;
		}

		@Override
		public String message() {
			return message;
		}
	}

	static class InvalidTestException extends BusinessException {

		InvalidTestException() {
			super(TestErrorCode.INVALID_TEST);
		}
	}

	static class SecondInvalidTestException extends BusinessException {

		SecondInvalidTestException() {
			super(TestErrorCode.SECOND_INVALID_TEST);
		}
	}

	static class ForbiddenTestException extends BusinessException {

		ForbiddenTestException() {
			super(TestErrorCode.FORBIDDEN_TEST);
		}
	}

	static class TestNotFoundException extends NotFoundException {

		TestNotFoundException() {
			super(TestErrorCode.TEST_NOT_FOUND);
		}
	}
}
