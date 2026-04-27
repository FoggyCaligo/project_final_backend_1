package com.today.fridge.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	/*
	 * success: 성공 여부를 판단합니다.
	 * code: HTTP 응답 코드를 전달합니다.
	 * message: 응답 메시지를 전달합니다.
	 * requestId: 응답 요청 ID를 전달합니다.
	 * data: 응답 데이터를 전달합니다.
	 * error: 응답 에러를 전달합니다.
	 */
	private Boolean success;
	private String code;
	private String message;
	private String requestId;
	private T data;
	private Object error;

	/*
	 * 성공 시 응답입니다.
	 * 프론트로 data만 전달하는 method와 data 및 (String) message를 전달하는 method가 존재합니다.
	 * 성공은 코드가 항상 200으로 전달되고 있습니다. 추가 코들가 필요하시면 그때 추가 method를 생성하도록 하겠습니다.
	 */
	public static <T> ApiResponse<T> success(T data) {
		return ApiResponse.<T>builder()
				.success(true)
				.code("200")
				.message("Success")
				.requestId(MDC.get("requestId"))
				.data(data)
				.build();
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return ApiResponse.<T>builder()
				.success(true)
				.code("200")
				.message(message)
				.requestId(MDC.get("requestId"))
				.data(data)
				.build();
	}

	/*
	 * 실패 시 응답입니다.
	 * code: ErrorCode Enum의 name() method를 전달합니다.
	 * message: 프론트에서 표시할 메시지입니다.
	 * errorDetails: 추가적인 에러 상세 정보입니다.
	 */
	public static <T> ApiResponse<T> error(String code, String message, Object errorDetails) {
		return ApiResponse.<T>builder()
				.success(false)
				.code(code)
				.message(message)
				.requestId(MDC.get("requestId"))
				.error(errorDetails)
				.build();
	}

	public static <T> ApiResponse<T> error(String code, String message) {
		return ApiResponse.<T>builder()
				.success(false)
				.code(code)
				.message(message)
				.requestId(MDC.get("requestId"))
				.build();
	}

	/*
	 * ====================================================================
	 * Meta class는 응답 데이터를 구조화하고 프론트와 통신하기 위해 생성하였습니다.
	 * success: 성공 여부를 판단합니다.
	 * code: HTTP 응답 코드를 전달합니다.
	 * message: 응답 메시지를 전달합니다.
	 * requestId: 응답 요청 ID를 전달합니다.
	 * ====================================================================
	 */
	@Getter
	@Builder
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Meta {

	}
}