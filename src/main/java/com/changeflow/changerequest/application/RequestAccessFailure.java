package com.changeflow.changerequest.application;

public final class RequestAccessFailure extends RuntimeException {
    public enum Code { FORBIDDEN, NOT_FOUND, CONCURRENT_MODIFICATION }
    private final Code code;
    public RequestAccessFailure(Code code) {super(code.name());this.code=code;}
    public Code code() {return code;}
}
