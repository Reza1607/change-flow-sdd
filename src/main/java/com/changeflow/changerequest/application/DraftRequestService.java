package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequest;
import com.changeflow.identity.application.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class DraftRequestService {
    private final CurrentActor actor;
    private final CurrentAccess access;
    private final ChangeRequestStore store;
    private final RequestMutationExecutor mutations;
    public DraftRequestService(CurrentActor actor,CurrentAccess access,ChangeRequestStore store,RequestMutationExecutor mutations) {
        this.actor=actor;this.access=access;this.store=store;this.mutations=mutations;
    }
    public ChangeRequest create(String title,String description,String reason) {
        var id=actor.require();RequestMutationExecutor.requireRequester(access.read(id));
        var draft=ChangeRequest.draft(UUID.randomUUID(),id.value(),title,description,reason);
        RequestMutationExecutor.requireRequester(access.guard(id));return store.create(draft);
    }
    public ChangeRequest edit(UUID id,long expectedVersion,String title,String description,String reason) {
        return mutations.edit(actor.require(),id,expectedVersion,title,description,reason);
    }
    public ChangeRequest submit(UUID id,long expectedVersion,String explanation) {
        return mutations.submit(actor.require(),id,expectedVersion,explanation);
    }
}
