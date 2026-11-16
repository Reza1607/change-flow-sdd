package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequestStatus;
import com.changeflow.identity.application.*;
import com.changeflow.identity.domain.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ListRequestsService {
    private final CurrentActor actor;
    private final CurrentAccess access;
    private final RequestReader reader;
    public ListRequestsService(CurrentActor actor,CurrentAccess access,RequestReader reader) {
        this.actor=actor;this.access=access;this.reader=reader;
    }
    // PostgreSQL's shared authorization guard needs a normal, short read transaction.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public RequestReader.Page list(ChangeRequestStatus status,int page,int size) {
        var snapshot=access.guard(actor.require());
        if(!snapshot.has(Role.Requester) && !snapshot.has(Role.Reviewer))
            throw new RequestAccessFailure(RequestAccessFailure.Code.FORBIDDEN);
        return reader.listVisible(snapshot,status,page,size);
    }
}
