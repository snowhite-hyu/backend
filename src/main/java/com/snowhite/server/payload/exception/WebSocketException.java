package com.snowhite.server.payload.exception;

import com.snowhite.server.payload.dto.ErrorReasonDTO;

public class WebSocketException extends RuntimeException {
    private final ErrorReasonDTO errorReason;

    public WebSocketException(ErrorReasonDTO errorReason) {
        super(errorReason.getMessage());
        this.errorReason = errorReason;
    }

    public ErrorReasonDTO getErrorReason() {
        return errorReason;
    }
}