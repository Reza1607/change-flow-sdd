package com.changeflow.changerequest.domain;

import java.util.List;

public final class DomainFailure extends RuntimeException {
    public enum Code { FORBIDDEN, SELF_REVIEW_FORBIDDEN, INVALID_TRANSITION, VALIDATION_FAILED }
    private final Code code;
    private final List<String> fields;
    public DomainFailure(Code code,List<String> fields) {super(code.name());this.code=code;this.fields=List.copyOf(fields);}
    public Code code() {return code;}
    public List<String> fields() {return fields;}
}
