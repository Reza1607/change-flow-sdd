package com.changeflow.changerequest.domain;

import java.util.List;
import java.util.UUID;

/** Pure transition rules. The application supplies the actor's freshly read required role. */
public final class TransitionPolicy {
    public enum Command { Submit, Approve, RequestChanges }
    private TransitionPolicy() {}
    public static void requireActor(ChangeRequest request,Command command,UUID actor,boolean authorized) {
        if(actor==null || !authorized) throw failure(DomainFailure.Code.FORBIDDEN);
        if(command==Command.Submit) {
            if(!request.requesterId().equals(actor)) throw failure(DomainFailure.Code.FORBIDDEN);
        } else if(request.requesterId().equals(actor)) throw failure(DomainFailure.Code.SELF_REVIEW_FORBIDDEN);
    }
    public static void requireState(ChangeRequest request,Command command) {
        boolean allowed=switch(command) {
            case Submit -> request.status()==ChangeRequestStatus.Draft || request.status()==ChangeRequestStatus.ChangesRequested;
            case Approve,RequestChanges -> request.status()==ChangeRequestStatus.PendingReview;
        };
        if(!allowed) throw failure(DomainFailure.Code.INVALID_TRANSITION);
    }
    public static ChangeRequest apply(ChangeRequest request,Command command,UUID actor,boolean authorized,String explanation) {
        requireActor(request,command,actor,authorized);
        requireState(request,command);
        if(command==Command.Submit) {
            try {return request.submit(actor);}
            catch(DraftRuleViolation failure) {
                throw new DomainFailure(DomainFailure.Code.valueOf(failure.code().name()),failure.fields());
            }
        }
        if(command==Command.RequestChanges && ChangeRequest.blank(explanation))
            throw new DomainFailure(DomainFailure.Code.VALIDATION_FAILED,List.of("explanation"));
        return new ChangeRequest(request.id(),request.title(),request.description(),request.reason(),request.requesterId(),
            command==Command.Approve?ChangeRequestStatus.Approved:ChangeRequestStatus.ChangesRequested,request.version());
    }
    private static DomainFailure failure(DomainFailure.Code code) {return new DomainFailure(code,List.of());}
}
