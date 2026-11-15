package com.changeflow.identity;

import com.changeflow.support.HttpIntegrationSupport;
import org.junit.jupiter.api.Test;
import java.net.http.HttpRequest;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.context.annotation.Import(com.changeflow.support.FoundationTestEndpoints.class)
class SecurityIT extends HttpIntegrationSupport {
    @Test
    void authenticatesDatabaseAccountAndProvidesCookieWithoutSession() throws Exception {
        var a=account(true,false);
        var r=send(request("/api/v1/csrf",a));
        assertEquals(200,r.statusCode());
        assertEquals("X-XSRF-TOKEN",json(r.body()).get("headerName").asString());
        assertFalse(json(r.body()).get("token").asString().isBlank());
        assertEquals("no-store",r.headers().firstValue("Cache-Control").orElseThrow());
        assertTrue(r.headers().allValues("Set-Cookie").stream().anyMatch(s->s.startsWith("XSRF-TOKEN=")));
        assertTrue(r.headers().allValues("Set-Cookie").stream().noneMatch(s->s.contains("JSESSIONID")));
    }
    @Test
    void missingAndInvalidCredentialsHaveUniformProblemAndChallenge() throws Exception {
        for(var b:java.util.List.of(request("/api/v1/csrf",null),request("/api/v1/csrf",account(false,false)).setHeader("Authorization","Basic bm9uZXhpc3RlbnQ6aW52YWxpZA=="))) {
            var r=send(b);assertEquals(401,r.statusCode());
            assertTrue(r.headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Basic"));
            assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
            assertEquals("UNAUTHENTICATED",json(r.body()).get("code").asString());
        }
    }
    @Test
    void authenticatedMutationNeedsMatchingCookieAndHeader() throws Exception {
        var a=account(true,false);
        var r=send(request("/api/v1/change-requests",a).POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertEquals(403,r.statusCode());
        assertEquals("CSRF_INVALID",json(r.body()).get("code").asString());
        var anonymous=send(request("/api/v1/change-requests",null).POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertEquals(401,anonymous.statusCode());
    }

    @Test
    void matchingCsrfAllowsTestMutationAndIdentityOnlyComesFromContext() throws Exception {
        var a=account(true,false);
        var token=send(request("/api/v1/csrf",a));
        var cookie=token.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        var value=json(token.body()).get("token").asString();
        var result=send(request("/test/identity?actorId="+java.util.UUID.randomUUID(),a)
            .header("Cookie",cookie).header("X-XSRF-TOKEN",value)
            .POST(HttpRequest.BodyPublishers.ofString("{\"actorId\":\"forged\",\"role\":\"Reviewer\"}")));
        assertEquals(200,result.statusCode());assertEquals(a.id().toString(),result.body());
        var wrong=send(request("/test/identity",a).header("Cookie",cookie).header("X-XSRF-TOKEN","wrong")
            .POST(HttpRequest.BodyPublishers.noBody()));
        assertEquals(403,wrong.statusCode());
        assertEquals("CSRF_INVALID",json(wrong.body()).get("code").asString());
        var param=send(request("/test/identity?_csrf="+value,a).header("Cookie",cookie).POST(HttpRequest.BodyPublishers.noBody()));
        assertEquals(403,param.statusCode());
        var noCookie=send(request("/test/identity",a).header("X-XSRF-TOKEN",value).POST(HttpRequest.BodyPublishers.noBody()));
        assertEquals(403,noCookie.statusCode());
        var logout=send(request("/logout",a).header("Cookie",cookie).header("X-XSRF-TOKEN",value).POST(HttpRequest.BodyPublishers.noBody()));
        assertEquals(404,logout.statusCode());
        var replay=send(request("/api/v1/csrf",null).header("Cookie",cookie));
        assertEquals(401,replay.statusCode());
        assertTrue(result.headers().allValues("Set-Cookie").stream().noneMatch(c->c.contains("JSESSIONID")));
    }
    @Test
    void forbiddenAndErrorsNeverReflectExceptionDetails() throws Exception {
        var a=account(true,false);
        var forbidden=send(request("/test/forbidden",a));assertEquals(403,forbidden.statusCode());
        assertEquals("FORBIDDEN",json(forbidden.body()).get("code").asString());
        for(int status:new int[]{400,404,409,422,500,503}) {
            var r=send(request("/test/error/"+status,a));assertEquals(status,r.statusCode());
            assertEquals("about:blank",json(r.body()).get("type").asString());
            assertFalse(r.body().contains("secret"));assertFalse(r.body().contains("private"));
            assertFalse(r.body().contains("stackTrace"));
        }
    }
    @Test
    void corsIsClosedAndDisabledAccountCannotAuthenticate() throws Exception {
        var a=account(true,false);
        var cross=send(request("/api/v1/csrf",a).header("Origin","https://untrusted.example"));
        assertEquals(403,cross.statusCode());
        assertTrue(cross.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertTrue(cross.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        jdbc.update("update identity_account set enabled=false where actor_id=?",a.id());
        assertEquals(401,send(request("/api/v1/csrf",a)).statusCode());
    }
}
