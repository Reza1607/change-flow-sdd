package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SchemaIT extends PostgresTestSupport {
    @Test
    void migratesEmptyDatabaseAndEnforcesConstraints() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load().migrate();
        try (var c = independentConnection(); var s = c.createStatement()) {
            try (var r = s.executeQuery("select count(*) from information_schema.tables where table_schema='public' and table_name in ('identity_account','change_request','status_history')")) {
                assertTrue(r.next()); assertEquals(3, r.getInt(1), "migration must create all three tables");
            }
            var actor = UUID.randomUUID(); var request = UUID.randomUUID();
            try (var p = c.prepareStatement("insert into identity_account(actor_id,username,password_hash,enabled,requester,reviewer) values (?,? ,?,true,true,false)")) {
                p.setObject(1,actor); p.setString(2,actor.toString()); p.setString(3,UUID.randomUUID().toString()); p.executeUpdate();
            }
            String prefix = "insert into change_request(id,requester_id,status,title) values ('";
            s.executeUpdate(prefix + request + "','" + actor + "','Draft',null)");
            s.executeUpdate(prefix + UUID.randomUUID() + "','" + actor + "','Draft','same')");
            s.executeUpdate(prefix + UUID.randomUUID() + "','" + actor + "','Draft','same')");
            try (var r=s.executeQuery("select version from change_request where id='"+request+"'")) { r.next(); assertEquals(0,r.getLong(1)); }
            assertConstraint(s, prefix+UUID.randomUUID()+"','"+actor+"','Invalid',null)","23514");
            assertConstraint(s, prefix+UUID.randomUUID()+"','"+UUID.randomUUID()+"','Draft',null)","23503");
            assertConstraint(s,"update change_request set version=null where id='"+request+"'","23502");
            assertConstraint(s,"update change_request set requester_id=null where id='"+request+"'","23502");
            String history="insert into status_history(id,request_id,actor_id,occurred_at,from_status,to_status,explanation,resulting_version) values ('";
            String row="','"+request+"','"+actor+"',current_timestamp,'Draft','PendingReview','',1)";
            s.executeUpdate(history+UUID.randomUUID()+row);
            assertConstraint(s,history+UUID.randomUUID()+row,"23505");
            assertConstraint(s,history+UUID.randomUUID()+"','"+request+"','"+actor+"',current_timestamp,'Approved','Draft','',2)","23514");
            assertConstraint(s,history+UUID.randomUUID()+"','"+request+"','"+actor+"',current_timestamp,'PendingReview','ChangesRequested','   ',2)","23514");
            assertConstraint(s,"delete from change_request where id='"+request+"'","23503");
            assertConstraint(s,"delete from identity_account where actor_id='"+actor+"'","23503");
            try(var r=s.executeQuery("select count(*) from pg_indexes where tablename='change_request' and (indexdef like '%(requester_id, id)%' or indexdef like '%(status, id)%')")) {r.next();assertEquals(2,r.getInt(1));}
            try(var r=s.executeQuery("select data_type from information_schema.columns where table_name='status_history' and column_name='occurred_at'")) {r.next();assertEquals("timestamp with time zone",r.getString(1));}
        }
    }
    private static void assertConstraint(Statement s,String sql,String state) {
        assertEquals(state,assertThrows(SQLException.class,()->s.executeUpdate(sql)).getSQLState());
    }
}
