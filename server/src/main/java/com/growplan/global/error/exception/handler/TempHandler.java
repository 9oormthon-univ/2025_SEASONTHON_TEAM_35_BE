package com.growplan.global.error.exception.handler;

import com.growplan.global.error.code.BaseErrorCode;
import com.growplan.global.error.exception.GeneralException;

public class TempHandler extends GeneralException {

    public TempHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
