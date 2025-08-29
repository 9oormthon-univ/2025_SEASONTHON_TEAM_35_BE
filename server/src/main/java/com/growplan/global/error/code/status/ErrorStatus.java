package com.growplan.global.error.code.status;

import com.growplan.global.error.code.BaseErrorCode;
import com.growplan.global.error.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // Member
    MEMBER_ROLE_INVALID(HttpStatus.FORBIDDEN, "MEMBER403", "해당 API 이용 권한이 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "멤버를 찾을 수 없습니다."),
    MEMBER_EXIST(HttpStatus.CONFLICT, "MEMBER409", "멤버가 이미 존재합니다."),

    // Jwt
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN400", "헤더에 토큰이 비어 있습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN401", "토큰이 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "TOKEN402", "토큰이 만료되었습니다. 재로그인해주세요."),
    INVALID_HEADER_FORMAT(HttpStatus.BAD_REQUEST, "TOKEN403", "헤더 형식이 올바르지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "TOKEN404", "리프레시 토큰을 찾을 수 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN405", "토큰이 만료되었습니다."),

    // OAuth
    OAUTH_TOKEN_FAIL(HttpStatus.BAD_REQUEST, "OAUTH400", "토큰 변경 실패"),
    OAUTH_USER_INFO_FAIL(HttpStatus.NOT_FOUND, "OAUTH401", "사용자 정보를 가져오지 못했습니다"),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH402", "올바르지 않은 플랫폼입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
