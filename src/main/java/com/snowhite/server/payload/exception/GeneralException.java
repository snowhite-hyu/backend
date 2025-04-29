package com.snowhite.server.payload.exception;

import com.snowhite.server.payload.code.BaseErrorCode;
import com.snowhite.server.payload.dto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private BaseErrorCode code;

    public ErrorReasonDTO getErrorReason() {
        return this.code.getErrorReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus() {
        return this.code.getErrorReasonHttpStatus();
    }
}

