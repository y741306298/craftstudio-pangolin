package com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth;

public class SmsCodeRequiredAuthException extends RuntimeException {
    public SmsCodeRequiredAuthException() {
        super();
    }

    public SmsCodeRequiredAuthException(String message) {
        super(message);
    }

    public SmsCodeRequiredAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmsCodeRequiredAuthException(Throwable cause) {
        super(cause);
    }

    protected SmsCodeRequiredAuthException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
