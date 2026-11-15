package com.changeflow.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class CsrfController {
    public record Token(String token,String headerName) {}
    @GetMapping("/api/v1/csrf")
    public Token token(CsrfToken token) {return new Token(token.getToken(),token.getHeaderName());}
}
