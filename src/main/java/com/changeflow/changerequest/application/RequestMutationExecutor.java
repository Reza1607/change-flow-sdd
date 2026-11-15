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
    private final RepeatedTransitionPolicy repeats;
    public RequestMutationExecutor(ChangeRequestStore store,AuditTrail audit,CurrentAccess access,Clock clock,RepeatedTransitionPolicy repeats) {
        this.store=store;this.audit=audit;this.access=access;this.clock=clock;this.repeats=repeats;
    }
    public ChangeRequest edit(ActorId actor,UUID id,long version,String title,String description,String reason) {
        return execute(actor,id,version,null,request->request.edit(actor.value(),title,description,reason),null);
    }
    public ChangeRequest submit(ActorId actor,UUID id,long version,String explanation) {
        return execute(actor,id,version,TransitionPolicy.Command.Submit,null,explanation);
    }
    public ChangeRequest approve(ActorId actor,UUID id,long version,String explanation) {
        return execute(actor,id,version,TransitionPolicy.Command.Approve,null,explanation);
    }
    private ChangeRequest execute(ActorId actor,UUID id,long version,TransitionPolicy.Command command,
            UnaryOperator<ChangeRequest> edit,String explanation) {
        var initial=access.read(actor);
        if(!initial.has(Role.Requester) && !initial.has(Role.Reviewer)) throw failure(RequestAccessFailure.Code.FORBIDDEN);
        var before=store.find(id).orElseThrow(()->failure(RequestAccessFailure.Code.NOT_FOUND));
        boolean visible=(initial.has(Role.Requester) && before.requesterId().equals(actor.value()))
            || (initial.has(Role.Reviewer) && before.status()==ChangeRequestStatus.PendingReview);
        if(!visible) {
            if(command==TransitionPolicy.Command.Approve) repeats.rejectSuccessfulApproval(before,actor,initial);
            throw failure(RequestAccessFailure.Code.NOT_FOUND);
        }
        var required=command==null || command==TransitionPolicy.Command.Submit?Role.Requester:Role.Reviewer;
        requireRole(initial,required);
        if(command==null) before.requireEditableBy(actor.value());
        else {
            TransitionPolicy.requireActor(before,command,actor.value(),initial.has(required));
            if(command==TransitionPolicy.Command.Submit) repeats.rejectRepeatedSubmit(before,actor,initial);
            TransitionPolicy.requireState(before,command);
        }
        if(version<0 || before.version()!=version) throw failure(RequestAccessFailure.Code.CONCURRENT_MODIFICATION);
        var replacement=command==null?edit.apply(before):TransitionPolicy.apply(before,command,actor.value(),initial.has(required),explanation);
        requireRole(access.guard(actor),required);
        var saved=store.update(replacement,version);
        if(command!=null) audit.append(new StatusHistoryEntry(UUID.randomUUID(),id,actor.value(),Instant.now(clock),
            before.status(),saved.status(),explanation==null?"":explanation,saved.version()));
        return saved;
    }
    static void requireRequester(AccessSnapshot snapshot) {requireRole(snapshot,Role.Requester);}
    private static void requireRole(AccessSnapshot snapshot,Role role) {
        if(!snapshot.has(role)) throw failure(RequestAccessFailure.Code.FORBIDDEN);
    }
    private static RequestAccessFailure failure(RequestAccessFailure.Code code) {return new RequestAccessFailure(code);}
}
