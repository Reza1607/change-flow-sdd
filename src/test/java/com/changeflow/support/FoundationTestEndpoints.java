package com.changeflow.support;

import com.changeflow.identity.application.CurrentActor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Test source set only; no product endpoint or fault switch is packaged. */
@TestConfiguration(proxyBeanMethods=false)
public class FoundationTestEndpoints {
    @org.springframework.boot.test.context.TestComponent
    @RestController
    static class ProbeController {
        private final CurrentActor actor;
        ProbeController(CurrentActor actor) {this.actor=actor;}
        @RequestMapping(value="/test/identity",method={RequestMethod.GET,RequestMethod.POST})
        String identity() {return actor.require().value().toString();}
        @GetMapping("/test/forbidden") String forbidden() {throw new org.springframework.security.access.AccessDeniedException("private data");}
        @GetMapping("/test/error/{status}") String error(@PathVariable int status) {
            if(status==500) throw new IllegalStateException("secret database exception");
            if(status==503) throw new org.springframework.dao.DataAccessResourceFailureException("secret SQL");
            throw new ResponseStatusException(HttpStatus.valueOf(status),"private detail");
        }
    }
}
