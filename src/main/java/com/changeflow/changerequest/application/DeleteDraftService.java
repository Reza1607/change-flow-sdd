package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequestStatus;
import com.changeflow.identity.application.*;
import com.changeflow.identity.domain.Role;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class DeleteDraftService {
    private final CurrentActor actor;
    private final CurrentAccess access;
    private final ChangeRequestStore store;
    public DeleteDraftService(CurrentActor actor,CurrentAccess access,ChangeRequestStore store) {
        this.actor=actor;this.access=access;this.store=store;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void delete(UUID id,long expectedVersion) {
        var currentActor=actor.require();var initial=access.read(currentActor);
        if(!initial.has(Role.Requester) && !initial.has(Role.Reviewer)) throw failure(RequestAccessFailure.Code.FORBIDDEN);
        var before=store.find(id).orElseThrow(()->failure(RequestAccessFailure.Code.NOT_FOUND));
        boolean visible=(initial.has(Role.Requester) && before.requesterId().equals(currentActor.value()))
            || (initial.has(Role.Reviewer) && before.status()==ChangeRequestStatus.PendingReview);
        if(!visible) throw failure(RequestAccessFailure.Code.NOT_FOUND);
        RequestMutationExecutor.requireRequester(initial);
        before.requireDeletableBy(currentActor.value());
        if(expectedVersion<0 || before.version()!=expectedVersion) throw failure(RequestAccessFailure.Code.CONCURRENT_MODIFICATION);
        RequestMutationExecutor.requireRequester(access.guard(currentActor));
        // The adapter retains the original expected version; a concurrent state change cannot be deleted.
        store.delete(id,expectedVersion);
    }
    private static RequestAccessFailure failure(RequestAccessFailure.Code code) {return new RequestAccessFailure(code);}
}
