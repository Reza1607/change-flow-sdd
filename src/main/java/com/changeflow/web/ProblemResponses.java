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
    /** Classify infrastructure failures without exposing exception text or losing a rollback's original cause. */
    static HttpStatus failureStatus(Throwable failure) {
        var pending=new java.util.ArrayDeque<Throwable>();
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        pending.add(failure);
        while(!pending.isEmpty()) {
            var cause=pending.removeFirst();
            if(!seen.add(cause)) continue;
            if(cause instanceof org.springframework.dao.DataAccessResourceFailureException
                    || cause instanceof org.springframework.dao.TransientDataAccessResourceException
                    || cause instanceof org.springframework.dao.RecoverableDataAccessException
                    || cause instanceof org.springframework.transaction.CannotCreateTransactionException)
                return HttpStatus.SERVICE_UNAVAILABLE;
            // Driver connection loss may survive a rollback only as the original SQLSTATE.
            if(cause instanceof java.sql.SQLException sql) {
                var state=sql.getSQLState();
                if(state!=null && (state.startsWith("08") || state.equals("57P01")))
                    return HttpStatus.SERVICE_UNAVAILABLE;
                if(sql.getNextException()!=null) pending.add(sql.getNextException());
            }
            if(cause.getCause()!=null) pending.add(cause.getCause());
            if(cause instanceof org.springframework.transaction.TransactionSystemException transaction
                    && transaction.getOriginalException()!=null) pending.add(transaction.getOriginalException());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
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
