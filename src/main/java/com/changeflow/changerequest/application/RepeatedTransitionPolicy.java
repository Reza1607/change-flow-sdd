package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.*;
import com.changeflow.identity.application.CurrentAccess;
import com.changeflow.identity.domain.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RepeatedTransitionPolicy {
    private final ApprovalHistory history;
    private final CurrentAccess access;
    public RepeatedTransitionPolicy(ApprovalHistory history,CurrentAccess access) {this.history=history;this.access=access;}
    public void rejectSuccessfulApproval(ChangeRequest request,ActorId actor,AccessSnapshot current) {
        if(current.has(Role.Reviewer) && request.status()==ChangeRequestStatus.Approved
                && history.hasApproval(request.id(),actor.value(),request.version())) {
            // The exceptional receipt path also requires a fresh guard, without publishing a DTO.
            if(!access.guard(actor).has(Role.Reviewer)) throw new RequestAccessFailure(RequestAccessFailure.Code.FORBIDDEN);
            throw invalidTransition();
        }
    }
    public void rejectRepeatedSubmit(ChangeRequest request,ActorId actor,AccessSnapshot current) {
        if(current.has(Role.Requester) && request.requesterId().equals(actor.value())
                && request.status()==ChangeRequestStatus.PendingReview) throw invalidTransition();
    }
    private static DomainFailure invalidTransition() {return new DomainFailure(DomainFailure.Code.INVALID_TRANSITION,List.of());}
}
