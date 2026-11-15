package com.changeflow.support;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Independent persisted history fixtures; never relies on another test's lifecycle. */
public abstract class HistoryApiSupport extends DraftApiSupport {
    protected void history(UUID id,Account actor,String from,String to,long version,String explanation) {
        jdbc.update("insert into status_history(id,request_id,actor_id,occurred_at,from_status,to_status,explanation,resulting_version) values (?,?,?,?,?,?,?,?)",
            UUID.randomUUID(),id,actor.id(),OffsetDateTime.parse("2026-11-15T10:00:00Z"),from,to,explanation,version);
    }
}
