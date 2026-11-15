package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.domain.*;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="change_request")
class ChangeRequestEntity {
    @Id @Column(nullable=false,updatable=false) private UUID id;
    @Column(columnDefinition="text") private String title;
    @Column(columnDefinition="text") private String description;
    @Column(columnDefinition="text") private String reason;
    @Column(name="requester_id",nullable=false,updatable=false) private UUID requesterId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,columnDefinition="text") private ChangeRequestStatus status;
    @Version @Column(nullable=false) private long version;
    protected ChangeRequestEntity() {}
    static ChangeRequestEntity from(ChangeRequest model) {
        var entity=new ChangeRequestEntity();
        entity.id=model.id();entity.title=model.title();entity.description=model.description();entity.reason=model.reason();
        entity.requesterId=model.requesterId();entity.status=model.status();entity.version=model.version();
        return entity;
    }
    ChangeRequest toDomain() {return new ChangeRequest(id,title,description,reason,requesterId,status,version);}
}
