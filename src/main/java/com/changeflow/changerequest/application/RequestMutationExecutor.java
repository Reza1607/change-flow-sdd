package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.*;
import com.changeflow.identity.application.CurrentAccess;
import com.changeflow.identity.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
@Transactional(propagation=Propagation.MANDATORY)
public class RequestMutationExecutor {
    private final ChangeRequestStore store;
    private final AuditTrail audit;
    private final CurrentAccess access;
    private final Clock clock;
    public RequestMutationExecutor(ChangeRequestStore store,AuditTrail audit,CurrentAccess access,Clock clock) {
        this.store=store;this.audit=audit;this.access=access;this.clock=clock;
    }
    public ChangeRequest edit(ActorId actor,UUID id,long version,String title,String description,String reason) {
        return execute(actor,id,version,false,request->request.edit(actor.value(),title,description,reason),null);
    }
    public ChangeRequest submit(ActorId actor,UUID id,long version,String explanation) {
        return execute(actor,id,version,true,request->request.submit(actor.value()),explanation);
    }
    private ChangeRequest execute(ActorId actor,UUID id,long version,boolean transition,
            UnaryOperator<ChangeRequest> mutation,String explanation) {
        var initial=access.read(actor);
        if(!initial.has(Role.Requester) && !initial.has(Role.Reviewer)) throw failure(RequestAccessFailure.Code.FORBIDDEN);
        var before=store.find(id).orElseThrow(()->failure(RequestAccessFailure.Code.NOT_FOUND));
        boolean visible=(initial.has(Role.Requester) && before.requesterId().equals(actor.value()))
            || (initial.has(Role.Reviewer) && before.status()==ChangeRequestStatus.PendingReview);
        if(!visible) throw failure(RequestAccessFailure.Code.NOT_FOUND);
        requireRequester(initial);
        if(transition) before.requireSubmittableBy(actor.value());else before.requireEditableBy(actor.value());
        if(version<0 || before.version()!=version) throw failure(RequestAccessFailure.Code.CONCURRENT_MODIFICATION);
        var replacement=mutation.apply(before); // Includes semantic validation, before any write or Audit.
        requireRequester(access.guard(actor));
        var saved=store.update(replacement,version);
        if(transition) audit.append(new StatusHistoryEntry(UUID.randomUUID(),id,actor.value(),Instant.now(clock),
            before.status(),saved.status(),explanation==null?"":explanation,saved.version()));
        return saved;
    }
    static void requireRequester(AccessSnapshot snapshot) {
        if(!snapshot.has(Role.Requester)) throw failure(RequestAccessFailure.Code.FORBIDDEN);
    }
    private static RequestAccessFailure failure(RequestAccessFailure.Code code) {return new RequestAccessFailure(code);}
}
