package com.gifo.backend.global;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 예시 - ApiTestController
    EXAMPLE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "예시 예외 발생"),

    // 기본 예외
    UNKNOWN_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류 요청 URL을 다시 확인해보십시오."),
    ILLEGAL_STATE(HttpStatus.INTERNAL_SERVER_ERROR, "잘못된 상태입니다."),
    NULL_POINTER(HttpStatus.BAD_REQUEST, "필수 데이터가 누락되었습니다."),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 파라미터 타입이 일치하지 않습니다."),
    NUMBER_FORMAT_ERROR(HttpStatus.BAD_REQUEST, "숫자 형식 오류입니다."),
    DB_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 접근 오류입니다."),
    NO_RESULT(HttpStatus.NOT_FOUND, "데이터를 찾을 수 없습니다."),
    EMPTY_RESULT(HttpStatus.NOT_FOUND, "결과가 존재하지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검증 실패"),

    // 이벤트 도메인
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),
    EVENT_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 URL 생성에 실패했습니다."),
    EVENT_EXPIRED(HttpStatus.GONE, "만료된 이벤트입니다."),
    EVENT_DELETED(HttpStatus.NOT_FOUND, "삭제된 이벤트입니다."),

    // 캡슐 도메인
    CAPSULE_NOT_FOUND(HttpStatus.NOT_FOUND, "캡슐 이벤트를 찾을 수 없습니다."),
    CAPSULE_DRAW_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "뽑기 가능 횟수를 초과했습니다."),
    CAPSULE_ALL_DRAWN(HttpStatus.BAD_REQUEST, "모든 캡슐을 이미 뽑았습니다."),
    CAPSULE_DRAW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 캡슐 뽑기 이력을 찾을 수 없습니다."),
    CAPSULE_ALREADY_SELECTED(HttpStatus.BAD_REQUEST, "이미 캡슐을 선택했습니다."),

    // 퀴즈 도메인
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈 이벤트를 찾을 수 없습니다."),

    // 외부 API 오류
    NAVER_API_ERROR(HttpStatus.BAD_GATEWAY, "네이버 쇼핑 API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    public String getCode() {
        return this.name();
    }
}