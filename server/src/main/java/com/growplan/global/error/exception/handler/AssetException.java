package com.growplan.global.error.exception.handler;

import com.growplan.global.error.code.BaseErrorCode;
import com.growplan.global.error.exception.GeneralException;

public class AssetException extends GeneralException {

    public AssetException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
