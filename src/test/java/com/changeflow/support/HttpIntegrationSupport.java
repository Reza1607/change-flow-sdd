package com.changeflow.support;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "server.ssl.enabled=false", "server.ssl.bundle=", "server.address=127.0.0.1",
    "spring.security.user.password=${random.uuid}"
})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class HttpIntegrationSupport extends PostgresTestSupport {
    @Value("${local.server.port}") protected int port;
    @Autowired protected JdbcTemplate jdbc;
    protected final HttpClient client=HttpClient.newHttpClient();
    protected record Account(UUID id,String username,String password) {}
    protected Account account(boolean requester,boolean reviewer) {
        var id=UUID.randomUUID(); var password=UUID.randomUUID().toString();
        jdbc.update("insert into identity_account(actor_id,username,password_hash,enabled,requester,reviewer) values (?,?,?,true,?,?)",
                id,id.toString(),PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password),requester,reviewer);
        return new Account(id,id.toString(),password);
    }
    protected HttpRequest.Builder request(String path,Account a) {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(a!=null) b.header("Authorization","Basic "+Base64.getEncoder().encodeToString((a.username()+":"+a.password()).getBytes(StandardCharsets.UTF_8)));
        return b;
    }
    protected HttpResponse<String> send(HttpRequest.Builder b) throws Exception {
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    protected JsonNode json(String text) {return JsonMapper.builder().build().readTree(text);}
}
