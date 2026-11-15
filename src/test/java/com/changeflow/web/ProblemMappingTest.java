package com.changeflow.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProblemMappingTest {
    @Test
    void standardProblemsHaveOnlySafeContractFields() {
        var cases=Map.of(400,"BAD_REQUEST",401,"UNAUTHENTICATED",403,"FORBIDDEN",404,"NOT_FOUND",
                409,"CONCURRENT_MODIFICATION",422,"VALIDATION_FAILED",500,"INTERNAL_ERROR",503,"DEPENDENCY_UNAVAILABLE");
        cases.forEach((status,code)-> {
            var p=ProblemResponses.create(HttpStatus.valueOf(status),code);
            assertEquals(status,p.getStatus());assertEquals(URI.create("about:blank"),p.getType());
            assertEquals(HttpStatus.valueOf(status).getReasonPhrase(),p.getTitle());
            assertEquals(Map.of("code",code),p.getProperties());
            assertNull(p.getDetail()); assertNull(p.getInstance());
        });
    }
}
