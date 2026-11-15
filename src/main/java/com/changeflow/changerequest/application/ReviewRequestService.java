package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequest;
import com.changeflow.identity.application.CurrentActor;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class ReviewRequestService {
    private final CurrentActor actor;
    private final RequestMutationExecutor mutations;
    public ReviewRequestService(CurrentActor actor,RequestMutationExecutor mutations) {this.actor=actor;this.mutations=mutations;}
    public ChangeRequest approve(UUID id,long expectedVersion,String explanation) {
        return mutations.approve(actor.require(),id,expectedVersion,explanation);
    }
}
