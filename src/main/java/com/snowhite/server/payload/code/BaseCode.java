package com.snowhite.server.payload.code;

import com.snowhite.server.payload.dto.ReasonDTO;

public interface BaseCode {

    ReasonDTO getReason();

    ReasonDTO getReasonHttpStatus();

}
