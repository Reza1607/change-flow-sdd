package com.changeflow.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class CsrfCookieTest {
    @Test
    void cookieFollowsTlsAndNeverBecomesAnAuthenticationSession() {
        var repository=new SecurityConfiguration().csrfTokenRepository();
        for(boolean tls:new boolean[]{true,false}) {
            var request=new MockHttpServletRequest();request.setSecure(tls);
            var response=new MockHttpServletResponse();
            repository.saveToken(repository.generateToken(request),request,response);
            var cookie=response.getCookie("XSRF-TOKEN");assertNotNull(cookie);
            assertEquals(tls,cookie.getSecure());assertTrue(cookie.isHttpOnly());
            assertEquals("Strict",cookie.getAttribute("SameSite"));assertNull(request.getSession(false));
        }
    }
}
