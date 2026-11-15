package com.changeflow.support;

import java.net.http.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

public abstract class DraftApiSupport extends HttpIntegrationSupport {
    protected static final String BASE="/api/v1/change-requests";
    protected HttpResponse<String> mutate(Account actor,String method,String path,String body) throws Exception {
        var token=send(request("/api/v1/csrf",actor));assertEquals(200,token.statusCode());
        return send(request(path,actor).header("Content-Type","application/json")
            .header("Cookie",token.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0])
            .header("X-XSRF-TOKEN",json(token.body()).get("token").asString())
            .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)));
    }
    protected String encode(Object value) {return JsonMapper.builder().build().writeValueAsString(value);}
    protected JsonNode create(Account actor,String body) throws Exception {
        var r=mutate(actor,"POST",BASE,body);assertEquals(201,r.statusCode());return json(r.body());
    }
    protected UUID seed(Account actor,String status,String title,String description,String reason,long version) {
        var id=UUID.randomUUID();jdbc.update("insert into change_request(id,requester_id,status,title,description,reason,version) values (?,?,?,?,?,?,?)",
            id,actor.id(),status,title,description,reason,version);return id;
    }
    protected long auditCount(UUID id) {return jdbc.queryForObject("select count(*) from status_history where request_id=?",Long.class,id);}
    protected Map<String,Object> snapshot(UUID id) {return jdbc.queryForMap("select * from change_request where id=?",id);}
    protected void problem(HttpResponse<String> response,int status,String code) {
        assertEquals(status,response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("about:blank",json(response.body()).get("type").asString());
        assertEquals(code,json(response.body()).get("code").asString());
    }
}
