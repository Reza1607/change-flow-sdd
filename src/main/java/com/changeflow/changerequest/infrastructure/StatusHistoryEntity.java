package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="status_history",uniqueConstraints=@UniqueConstraint(columnNames={"request_id","resulting_version"}))
class StatusHistoryEntity {
    @Id @Column(nullable=false,updatable=false) private UUID id;
    @Column(name="request_id",nullable=false,updatable=false) private UUID requestId;
    @Column(name="actor_id",nullable=false,updatable=false) private UUID actorId;
    @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt;
    @Enumerated(EnumType.STRING) @Column(name="from_status",nullable=false,updatable=false,columnDefinition="text") private ChangeRequestStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name="to_status",nullable=false,updatable=false,columnDefinition="text") private ChangeRequestStatus toStatus;
    @Column(nullable=false,updatable=false,columnDefinition="text") private String explanation;
    @Column(name="resulting_version",nullable=false,updatable=false) private long resultingVersion;
    protected StatusHistoryEntity() {}
    static StatusHistoryEntity from(StatusHistoryEntry model) {
        var entity=new StatusHistoryEntity();
        entity.id=model.id();entity.requestId=model.requestId();entity.actorId=model.actorId();entity.occurredAt=model.occurredAt();
        entity.fromStatus=model.fromStatus();entity.toStatus=model.toStatus();entity.explanation=model.explanation();entity.resultingVersion=model.resultingVersion();
        return entity;
    }
    StatusHistoryEntry toDomain() {return new StatusHistoryEntry(id,requestId,actorId,occurredAt,fromStatus,toStatus,explanation,resultingVersion);}
}
