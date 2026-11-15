package com.changeflow.changerequest.application;

import com.changeflow.identity.application.*;
import com.changeflow.identity.domain.Role;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HistoryQueryService {
    private final CurrentActor actor;
    private final CurrentAccess access;
    private final HistoryReader reader;
    public HistoryQueryService(CurrentActor actor,CurrentAccess access,HistoryReader reader) {
        this.actor=actor;this.access=access;this.reader=reader;
    }
    // The shared authorization guard requires a normal transaction, retained until the complete history result has been read.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public HistoryReader.Page list(UUID id,int page,int size) {
        var snapshot=access.guard(actor.require());
        if(!snapshot.has(Role.Requester) && !snapshot.has(Role.Reviewer))
            throw new RequestAccessFailure(RequestAccessFailure.Code.FORBIDDEN);
        return reader.findVisible(id,snapshot,page,size)
            .orElseThrow(()->new RequestAccessFailure(RequestAccessFailure.Code.NOT_FOUND));
    }
}
