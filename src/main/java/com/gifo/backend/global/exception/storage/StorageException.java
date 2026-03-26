package com.gifo.backend.global.exception.storage;

import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.CustomException;

public class StorageException extends CustomException {

    public StorageException(ErrorCode errorCode) {
        super(errorCode);
    }
}
