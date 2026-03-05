package com.gifo.backend.global.exception.event;

import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.CustomException;

public class EventException extends CustomException {

    public EventException(ErrorCode errorCode) {
        super(errorCode);
    }
}
