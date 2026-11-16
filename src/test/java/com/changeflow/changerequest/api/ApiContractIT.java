package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import com.changeflow.identity.infrastructure.IdentityFaultConfiguration;
import com.changeflow.identity.infrastructure.IdentityFaultConfiguration.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP + PostgreSQL, with schemas and advertised responses read from the approved contract. */
@Import(IdentityFaultConfiguration.class)
class ApiContractIT extends DraftApiSupport {
    @Autowired Faults faults;
    private final Map<String,Object> contract=loadContract();
    private final Set<String> exercised=new HashSet<>();
    private record Call(String operation,String method,String path,String body) {}
    private Map<String,Object> loadContract() {
        try(var input=Files.newInputStream(Path.of("specs/001-change-request-management/contracts/openapi.yaml"))) {
            return new Yaml().load(input);
        } catch(Exception ex) {throw new IllegalStateException(ex);}
    }
    @SuppressWarnings("unchecked") private Map<String,Object> map(Object o) {return (Map<String,Object>)o;}
    private Map<String,Object> resolve(Object value) {
        var result=map(value);
        if(result.containsKey("$ref")) {
            Object node=contract;
            for(var part:result.get("$ref").toString().substring(2).split("/"))node=map(node).get(part);
            return resolve(node);
        }
        return result;
    }
    private Map<String,Object> operation(String name) {
        for(var path:map(contract.get("paths")).values())for(var entry:map(path).entrySet())
            if(Set.of("get","post","put","delete").contains(entry.getKey()) && name.equals(map(entry.getValue()).get("operationId")))return map(entry.getValue());
        throw new AssertionError("Unknown operationId "+name);
    }
    private HttpRequest.Builder prepared(Call call,Account actor,boolean csrf) throws Exception {
        boolean declared=false;
        String server=map(((List<?>)contract.get("servers")).getFirst()).get("url").toString();
        for(var path:map(contract.get("paths")).entrySet()) {
            var candidate=map(path.getValue()).get(call.method().toLowerCase(Locale.ROOT));
            if(candidate!=null && call.operation().equals(map(candidate).get("operationId"))) {
                String pattern=java.util.regex.Pattern.quote(server+path.getKey()).replace("{id}","\\E[^/]+\\Q");
                assertTrue(java.net.URI.create(call.path()).getPath().matches(pattern),"HTTP path differs from OpenAPI: "+call.operation());
                declared=true;
            }
        }
        assertTrue(declared,"HTTP method differs from OpenAPI: "+call.operation());
        var builder=request(call.path(),actor);
        if(call.body()!=null)builder.header("Content-Type","application/json");
        if(csrf) {
            var token=send(request("/api/v1/csrf",actor));assertEquals(200,token.statusCode());
            builder.header("Cookie",token.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0]);
            builder.header("X-XSRF-TOKEN",json(token.body()).get("token").asString());
        }
        return builder.method(call.method(),call.body()==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(call.body()));
    }
    private HttpResponse<String> invoke(Call call,Account actor,int status) throws Exception {
        var response=send(prepared(call,actor,!call.method().equals("GET")));
        check(call.operation(),response,status);
        if(status<300 && call.body()!=null) {
            var input=map(map(operation(call.operation()).get("requestBody")).get("content"));
            schema(JsonMapper.builder().build().readValue(call.body(),Object.class),map(input.get("application/json")).get("schema"));
        }
        return response;
    }
    private void check(String op,HttpResponse<String> response,int expected) {
        assertEquals(expected,response.statusCode(),op+" "+response.body());
        var responses=map(operation(op).get("responses"));
        assertTrue(responses.containsKey(""+expected),op+" must advertise "+expected);
        var definition=resolve(responses.get(""+expected));exercised.add(op);
        assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow(),op);
        if(definition.containsKey("headers"))for(var header:map(definition.get("headers")).entrySet()) {
            var value=response.headers().firstValue(header.getKey()).orElseThrow();
            schema(value,map(header.getValue()).get("schema"));
        }
        if(!definition.containsKey("content")) {
            assertEquals("",response.body());assertTrue(response.headers().firstValue("Content-Type").isEmpty());return;
        }
        var content=map(definition.get("content"));var media=response.headers().firstValue("Content-Type").orElseThrow().split(";",2)[0];
        assertTrue(content.containsKey(media),op+" unexpected content type "+media);
        var body=JsonMapper.builder().build().readValue(response.body(),Object.class);
        schema(body,map(content.get(media)).get("schema"));
        if(expected>=400) {
            assertEquals(expected,((Number)map(body).get("status")).intValue());
            assertEquals("about:blank",map(body).get("type"));
            assertEquals(org.springframework.http.HttpStatus.valueOf(expected).getReasonPhrase(),map(body).get("title"));
            var codes=switch(expected) {
                case 400->Set.of("BAD_REQUEST");case 401->Set.of("UNAUTHENTICATED");
                case 403->Set.of("FORBIDDEN","SELF_REVIEW_FORBIDDEN","CSRF_INVALID");
                case 404->Set.of("NOT_FOUND");case 409->Set.of("INVALID_TRANSITION","CONCURRENT_MODIFICATION");
                case 422->Set.of("VALIDATION_FAILED");case 500->Set.of("INTERNAL_ERROR");
                case 503->Set.of("DEPENDENCY_UNAVAILABLE");default->Set.of();
            };
            assertTrue(codes.contains(map(body).get("code")),op+" error code");
            if(expected==401)assertTrue(response.headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Basic "));
            assertFalse(response.body().contains("PRIVATE_IDENTITY_FAILURE"));
        }
    }
    private void schema(Object value,Object raw) {
        var schema=resolve(raw);Object type=schema.get("type");
        if(value==null) {assertTrue(type instanceof List<?> types && types.contains("null"));return;}
        String kind=type instanceof List<?> types?types.stream().filter(t->!t.equals("null")).findFirst().orElseThrow().toString():type.toString();
        switch(kind) {
            case "object" -> {
                assertInstanceOf(Map.class,value);var object=map(value);
                for(var required:(List<?>)schema.getOrDefault("required",List.of()))assertTrue(object.containsKey(required),"missing "+required);
                var properties=map(schema.getOrDefault("properties",Map.of()));
                if(Boolean.FALSE.equals(schema.get("additionalProperties")))assertTrue(properties.keySet().containsAll(object.keySet()),"unexpected response properties "+object.keySet());
                for(var field:object.entrySet())if(properties.containsKey(field.getKey()))schema(field.getValue(),properties.get(field.getKey()));
            }
            case "array" -> {assertInstanceOf(List.class,value);for(var item:(List<?>)value)schema(item,schema.get("items"));}
            case "integer" -> {assertInstanceOf(Number.class,value);assertEquals(((Number)value).doubleValue(),((Number)value).longValue());}
            case "string" -> assertInstanceOf(String.class,value);
            default -> fail("Unsupported schema type "+kind);
        }
        if(schema.containsKey("const"))assertEquals(schema.get("const"),value);
        if(schema.containsKey("enum"))assertTrue(((List<?>)schema.get("enum")).contains(value));
        if(schema.containsKey("minimum"))assertTrue(((Number)value).doubleValue()>=((Number)schema.get("minimum")).doubleValue());
        if(schema.containsKey("maximum"))assertTrue(((Number)value).doubleValue()<=((Number)schema.get("maximum")).doubleValue());
        if(schema.containsKey("minLength"))assertTrue(value.toString().length()>=((Number)schema.get("minLength")).intValue());
        if("uuid".equals(schema.get("format")))assertDoesNotThrow(()->UUID.fromString(value.toString()));
        if("date-time".equals(schema.get("format")))assertEquals(0,OffsetDateTime.parse(value.toString()).getOffset().getTotalSeconds());
    }
    private List<Call> calls(UUID id) {
        String path=BASE+"/"+id;
        return List.of(new Call("getCsrf","GET","/api/v1/csrf",null),new Call("listRequests","GET",BASE,null),
            new Call("createDraft","POST",BASE,"{}"),new Call("getRequest","GET",path,null),
            new Call("editRequest","PUT",path,"{\"expectedVersion\":0,\"title\":null,\"description\":null,\"reason\":null}"),
            new Call("deleteDraft","DELETE",path+"?expectedVersion=0",null),
            new Call("listHistory","GET",path+"/history",null),
            new Call("submitRequest","POST",path+"/submit","{\"expectedVersion\":0}"),
            new Call("approveRequest","POST",path+"/approve","{\"expectedVersion\":0}"),
            new Call("requestChanges","POST",path+"/request-changes","{\"expectedVersion\":0,\"explanation\":\"correct\"}"));
    }
    @Test void allTenSuccessOperationsAndPriorApprovalReceiptMatchContract() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);
        var csrf=invoke(calls(UUID.randomUUID()).getFirst(),owner,200);
        assertTrue(csrf.headers().firstValue("Set-Cookie").orElseThrow().startsWith("XSRF-TOKEN="));
        assertFalse(csrf.headers().allValues("Set-Cookie").stream().anyMatch(v->v.contains("JSESSIONID")));
        var created=invoke(new Call("createDraft","POST",BASE,"{}"),owner,201);
        var id=UUID.fromString(json(created.body()).get("id").asString());var path=BASE+"/"+id;
        assertEquals(path,created.headers().firstValue("Location").orElseThrow());
        assertEquals(owner.id().toString(),json(created.body()).get("requesterId").asString());
        invoke(new Call("editRequest","PUT",path,"{\"expectedVersion\":0,\"title\":\"t\",\"description\":\"d\",\"reason\":\"r\"}"),owner,200);
        invoke(new Call("submitRequest","POST",path+"/submit","{\"expectedVersion\":1}"),owner,200);
        invoke(new Call("requestChanges","POST",path+"/request-changes","{\"expectedVersion\":2,\"explanation\":\"correct\"}"),reviewer,200);
        invoke(new Call("submitRequest","POST",path+"/submit","{\"expectedVersion\":3}"),owner,200);
        var approve=new Call("approveRequest","POST",path+"/approve","{\"expectedVersion\":4}");
        invoke(approve,reviewer,200);invoke(approve,reviewer,409);
        invoke(new Call("getRequest","GET",path,null),reviewer,404);
        invoke(new Call("getRequest","GET",path,null),owner,200);
        var history=invoke(new Call("listHistory","GET",path+"/history",null),owner,200);
        assertEquals(4,json(history.body()).get("totalElements").asInt());
        invoke(new Call("listRequests","GET",BASE,null),owner,200);
        var draft=seed(owner,"Draft",null,null,null,0);
        invoke(new Call("deleteDraft","DELETE",BASE+"/"+draft+"?expectedVersion=0",null),owner,204);
        var advertised=new HashSet<String>();
        for(var p:map(contract.get("paths")).values())for(var e:map(p).entrySet())
            if(Set.of("get","post","put","delete").contains(e.getKey()))advertised.add(map(e.getValue()).get("operationId").toString());
        assertEquals(10,advertised.size());assertEquals(advertised,exercised);
    }
    @Test void authenticationCsrfAndIdentityFaultsMatchEveryOperationWithoutEffects() throws Exception {
        var owner=account(true,true);var id=seed(owner,"Draft","t","d","r",0);var before=snapshot(id);
        var initialCount=jdbc.queryForObject("select count(*) from change_request",Long.class);
        for(var call:calls(id)) {
            check(call.operation(),send(prepared(call,null,false)),401);
            if(!call.method().equals("GET"))check(call.operation(),send(prepared(call,owner,false)),403);
            for(var failure:List.of(Failure.Connection,Failure.Unexpected)) {
                var builder=prepared(call,owner,!call.method().equals("GET"));faults.next.set(failure);
                check(call.operation(),send(builder),failure==Failure.Connection?503:500);
                assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
                assertEquals(initialCount,jdbc.queryForObject("select count(*) from change_request",Long.class));
            }
        }
    }
    @Test void unknownFieldsTypesAndParametersAreRejectedAndSemanticErrorsRemainExplicit() throws Exception {
        var owner=account(true,true);var reviewer=account(false,true);var noRole=account(false,false);
        var id=seed(owner,"Draft",null,null,null,0);var before=snapshot(id);
        for(var call:calls(id))if(call.body()!=null) {
            var unknown=call.body().substring(0,call.body().length()-1)+(call.body().equals("{}")?"":",")+"\"unexpected\":true}";
            invoke(new Call(call.operation(),call.method(),call.path(),unknown),owner,400);
            invoke(new Call(call.operation(),call.method(),call.path(),"{"),owner,400);
            if(!call.operation().equals("createDraft"))invoke(new Call(call.operation(),call.method(),call.path(),call.body().replace("\"expectedVersion\":0","\"expectedVersion\":\"invalid\"")),owner,400);
        }
        for(var suffix:List.of("?page=-1","?size=0","?size=101","?status=invalid"))invoke(new Call("listRequests","GET",BASE+suffix,null),owner,400);
        invoke(new Call("deleteDraft","DELETE",BASE+"/"+id,null),owner,400);
        invoke(new Call("submitRequest","POST",BASE+"/"+id+"/submit","{}"),owner,400);
        invoke(new Call("submitRequest","POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}"),owner,422);
        invoke(new Call("createDraft","POST",BASE,"{}"),noRole,403);
        invoke(new Call("editRequest","PUT",BASE+"/"+id,"{\"expectedVersion\":1,\"title\":null,\"description\":null,\"reason\":null}"),owner,409);
        invoke(new Call("getRequest","GET",BASE+"/"+id,null),reviewer,404);
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        var pending=seed(owner,"PendingReview","t","d","r",0);
        invoke(new Call("requestChanges","POST",BASE+"/"+pending+"/request-changes","{\"expectedVersion\":0,\"explanation\":\" \\u00a0\\u2003\"}"),reviewer,422);
        assertEquals(0,auditCount(pending));assertEquals("PendingReview",snapshot(pending).get("status"));
    }
    @Test void emptyQueryValuesAreNotValidIntegerOrStatusParameters() throws Exception {
        var owner=account(true,false);var id=seed(owner,"Draft","t","d","r",0);
        var cases=List.of(new Call("listRequests","GET",BASE+"?page=",null),
            new Call("listRequests","GET",BASE+"?size=",null),
            new Call("listRequests","GET",BASE+"?status=",null),
            new Call("listHistory","GET",BASE+"/"+id+"/history?page=",null),
            new Call("listHistory","GET",BASE+"/"+id+"/history?size=",null));
        assertAll(cases.stream().map(call->(org.junit.jupiter.api.function.Executable)()->invoke(call,owner,400)));
    }

}
