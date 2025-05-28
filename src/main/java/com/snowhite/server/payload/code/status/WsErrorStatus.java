package com.snowhite.server.payload.code.status;

import com.snowhite.server.payload.code.BaseErrorCode;
import com.snowhite.server.payload.dto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WsErrorStatus implements BaseErrorCode{
    INTERNAL_ERROR("WS500", "알 수 없는 서버 오류가 발생했습니다."),
    BAD_REQUEST("WS400", "잘못된 요청입니다."),
    CANNOT_USE_CARD("BUSINESS001", "이 카드를 사용할 수 없습니다.");

    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getErrorReason() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDTO getErrorReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .httpStatus(null)
                .build();
    }

}
