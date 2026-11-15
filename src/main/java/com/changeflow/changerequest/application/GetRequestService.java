package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequest;
import com.changeflow.identity.application.*;
import com.changeflow.identity.domain.Role;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class GetRequestService {
    private final CurrentActor actor;
    private final CurrentAccess access;
    private final RequestReader reader;
    public GetRequestService(CurrentActor actor,CurrentAccess access,RequestReader reader) {this.actor=actor;this.access=access;this.reader=reader;}
    // PostgreSQL FOR SHARE needs a normal transaction even though the application only reads.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ChangeRequest get(UUID id) {
        var snapshot=access.guard(actor.require());
        if(!snapshot.has(Role.Requester) && !snapshot.has(Role.Reviewer))
            throw new RequestAccessFailure(RequestAccessFailure.Code.FORBIDDEN);
        return reader.findVisible(id,snapshot).orElseThrow(()->new RequestAccessFailure(RequestAccessFailure.Code.NOT_FOUND));
    }
}
