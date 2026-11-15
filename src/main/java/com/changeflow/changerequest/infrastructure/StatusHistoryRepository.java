package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.ApprovalHistory;
import com.changeflow.changerequest.domain.ChangeRequestStatus;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class StatusHistoryRepository implements ApprovalHistory {
    private final EntityManager em;
    public StatusHistoryRepository(EntityManager em) {this.em=em;}
    public boolean hasApproval(UUID requestId,UUID actorId,long resultingVersion) {
        return em.createQuery("""
            select count(h) from StatusHistoryEntity h
            where h.requestId=:request and h.actorId=:actor and h.resultingVersion=:version
            and h.fromStatus=:source and h.toStatus=:target
            """,Long.class).setParameter("request",requestId).setParameter("actor",actorId)
            .setParameter("version",resultingVersion).setParameter("source",ChangeRequestStatus.PendingReview)
            .setParameter("target",ChangeRequestStatus.Approved).getSingleResult()>0;
    }
}
