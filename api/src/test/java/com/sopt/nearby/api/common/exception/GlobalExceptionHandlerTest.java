// 공통 응답과 전역 예외 처리 동작을 검증하는 테스트
package com.sopt.nearby.api.common.exception;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.api.common.response.CommonResponse;
import com.sopt.nearby.api.common.response.SuccessCode;
import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void wrapsSuccessResponse() throws Exception {
		mockMvc.perform(get("/test/success"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.code").value("TEST_SUCCESS"))
			.andExpect(jsonPath("$.message").value("테스트 성공입니다."))
			.andExpect(jsonPath("$.data.value").value("ok"));
	}

	@Test
	void handlesBusinessExceptionWithErrorCode() throws Exception {
		mockMvc.perform(get("/test/business-error"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("TEST_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("테스트 대상을 찾을 수 없습니다."))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	void handlesValidationException() throws Exception {
		mockMvc.perform(post("/test/validation-error")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("이름은 필수입니다."))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	void handlesUnhandledException() throws Exception {
		mockMvc.perform(get("/test/unhandled-error"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("내부 서버 오류가 발생했습니다. 다시 시도해 주세요."))
			.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/success")
		CommonResponse<TestResponse> success() {
			return CommonResponse.success(TestSuccessCode.TEST_SUCCESS, new TestResponse("ok"));
		}

		@GetMapping("/test/business-error")
		void businessError() {
			throw new TestBusinessException();
		}

		@PostMapping("/test/validation-error")
		void validationError(@Valid @RequestBody final TestRequest request) {
		}

		@GetMapping("/test/unhandled-error")
		void unhandledError() {
			throw new IllegalStateException("unexpected");
		}
	}

	record TestResponse(String value) {
	}

	record TestRequest(
		@NotBlank(message = "이름은 필수입니다.")
		String name
	) {
	}

	enum TestSuccessCode implements SuccessCode {
		TEST_SUCCESS("테스트 성공입니다.");

		private final String message;

		TestSuccessCode(final String message) {
			this.message = message;
		}

		@Override
		public String message() {
			return message;
		}
	}

	enum TestErrorCode implements ErrorCode {
		TEST_NOT_FOUND(404, "테스트 대상을 찾을 수 없습니다.");

		private final int status;
		private final String message;

		TestErrorCode(final int status, final String message) {
			this.status = status;
			this.message = message;
		}

		@Override
		public int status() {
			return status;
		}

		@Override
		public String message() {
			return message;
		}
	}

	static class TestBusinessException extends BusinessException {

		TestBusinessException() {
			super(TestErrorCode.TEST_NOT_FOUND);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}
}
