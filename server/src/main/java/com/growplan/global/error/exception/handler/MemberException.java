package com.growplan.global.error.exception.handler;

import com.growplan.global.error.code.BaseErrorCode;
import com.growplan.global.error.exception.GeneralException;

public class MemberException extends GeneralException {

    public MemberException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
