package com.gifo.backend.global.exception.capsule;

import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.CustomException;

public class CapsuleException extends CustomException {
    public CapsuleException(ErrorCode errorCode) {
        super(errorCode);
    }
}
