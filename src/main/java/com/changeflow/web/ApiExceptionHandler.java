package com.changeflow.web;

import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,Object body,HttpHeaders headers,
            HttpStatusCode status,WebRequest request) {
        var safe=new HttpHeaders();safe.putAll(headers);safe.setCacheControl("no-store");
        return new ResponseEntity<>(ProblemResponses.create(HttpStatus.valueOf(status.value()),ProblemResponses.codeFor(status.value())),safe,status);
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> authentication(AuthenticationException ex) {return response(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED");}
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> forbidden(AccessDeniedException ex) {return response(HttpStatus.FORBIDDEN,"FORBIDDEN");}
    @ExceptionHandler({DataAccessResourceFailureException.class,CannotCreateTransactionException.class})
    ResponseEntity<Object> unavailable(Exception ex) {return response(HttpStatus.SERVICE_UNAVAILABLE,"DEPENDENCY_UNAVAILABLE");}
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> conflict(Exception ex) {return response(HttpStatus.CONFLICT,"CONCURRENT_MODIFICATION");}
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex) {return response(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR");}
    private ResponseEntity<Object> response(HttpStatus status,String code) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).cacheControl(CacheControl.noStore())
                .body(ProblemResponses.create(status,code));
    }
}
