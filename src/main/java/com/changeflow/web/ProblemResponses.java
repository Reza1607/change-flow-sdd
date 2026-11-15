package com.changeflow.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/** Shared representation for MVC and the Security filter chain; no exception messages are reflected. */
public final class ProblemResponses {
    private ProblemResponses() {}
    public static ProblemDetail create(HttpStatus status,String code) {
        var p=ProblemDetail.forStatus(status);
        p.setType(java.net.URI.create("about:blank"));
        p.setTitle(status.getReasonPhrase());
        p.setDetail(null);
        p.setProperty("code",code);
        return p;
    }
    public static void write(HttpServletResponse response,ObjectMapper mapper,HttpStatus status,String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control","no-store");
        mapper.writeValue(response.getOutputStream(),create(status,code));
    }
    static String codeFor(int status) {
        return switch(status) {
            case 400 -> "BAD_REQUEST"; case 401 -> "UNAUTHENTICATED"; case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND"; case 409 -> "CONCURRENT_MODIFICATION";
            case 422 -> "VALIDATION_FAILED"; case 503 -> "DEPENDENCY_UNAVAILABLE";
            default -> "INTERNAL_ERROR";
        };
    }
}
