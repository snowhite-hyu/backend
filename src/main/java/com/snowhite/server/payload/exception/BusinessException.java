package com.snowhite.server.payload.exception;

import com.snowhite.server.payload.code.status.WsErrorStatus;
import com.snowhite.server.payload.dto.ErrorReasonDTO;

public class BusinessException extends RuntimeException{
    private final WsErrorStatus errorStatus;

    public BusinessException(WsErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }

    public WsErrorStatus getErrorStatus() {
        return errorStatus;
    }

    public ErrorReasonDTO getErrorReason() {
        return errorStatus.getErrorReason();
    }
}
