package com.snowhite.server.payload.code;

import com.snowhite.server.payload.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getErrorReason();

    ErrorReasonDTO getErrorReasonHttpStatus();
}