package com.changeflow.changerequest.api;

import com.changeflow.support.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.context.annotation.Import;
import java.net.http.HttpRequest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
@Import(FoundationTestEndpoints.class)
@org.springframework.test.context.TestPropertySource(properties="debug=true")
class LogPrivacyIT extends HttpIntegrationSupport {
    @Test
    void logsStructuredOutcomeAndNeverLogsSensitiveInputOrCredentials(CapturedOutput captured) throws Exception {
        var a=account(true,false);
        var marker="PRIVATE_"+UUID.randomUUID();
        var token=send(request("/api/v1/csrf",a));assertEquals(200,token.statusCode());
        var value=json(token.body()).get("token").asString();
        var cookie=token.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        var response=send(request("/test/identity?reason="+marker,a).header("Cookie",cookie).header("X-XSRF-TOKEN",value)
            .header("X-Correlation-ID",marker)
            .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\""+marker+"\",\"description\":\""+marker+"\",\"reason\":\""+marker+"\",\"explanation\":\""+marker+"\"}")));
        assertEquals(200,response.statusCode());
        assertEquals(403,send(request("/test/identity",a).POST(HttpRequest.BodyPublishers.ofString(marker))).statusCode());
        assertEquals(401,send(request("/api/v1/csrf",null)).statusCode());
        // Wait for the server-side finally block after the HTTP response has been sent.
        var logs=new ArrayList<tools.jackson.databind.JsonNode>();
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline) {
            logs.clear();
            for(var line:captured.getAll().split("\\R")) if(line.startsWith("{") && line.contains("RequestLoggingFilter")) logs.add(json(line));
            if(logs.size()>=4) break;
            Thread.onSpinWait();
        }
        assertTrue(logs.size()>=4,"structured HTTP events must be present");
        var ids=new HashSet<String>();var outcomes=new HashSet<String>();
        for(var event:logs) {
            ids.add(UUID.fromString(event.get("correlationId").asString()).toString());
            assertFalse(event.get("operation").asString().isBlank());
            assertTrue(event.get("duration").asLong()>=0);
            outcomes.add(event.get("outcome").asString());
        }
        assertEquals(logs.size(),ids.size());assertTrue(outcomes.containsAll(Set.of("200","401","403")));
        var text=captured.getAll();
        for(var secret:List.of(marker,a.password(),value,Base64.getEncoder().encodeToString((a.username()+":"+a.password()).getBytes(java.nio.charset.StandardCharsets.UTF_8))))
            assertFalse(text.contains(secret),"sensitive content must not be logged");
        assertFalse(text.contains("Authorization:"));
    }
}
