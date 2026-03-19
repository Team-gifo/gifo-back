package com.gifo.backend.global.exception.quiz;

import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.CustomException;

public class QuizException extends CustomException {
    public QuizException(ErrorCode errorCode) {
        super(errorCode);
    }
}
