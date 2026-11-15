package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.RequestReader;
import com.changeflow.changerequest.domain.*;
import com.changeflow.identity.domain.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JpaRequestReader implements RequestReader {
    private final EntityManager em;
    public JpaRequestReader(EntityManager em) {this.em=em;}
    public Optional<ChangeRequest> findVisible(UUID id,AccessSnapshot access) {
        return em.createQuery("""
            select r from ChangeRequestEntity r where r.id=:id and
            ((:requester=true and r.requesterId=:actor) or (:reviewer=true and r.status=:pending))
            """,ChangeRequestEntity.class)
            .setParameter("id",id).setParameter("actor",access.actorId().value())
            .setParameter("requester",access.has(Role.Requester)).setParameter("reviewer",access.has(Role.Reviewer))
            .setParameter("pending",ChangeRequestStatus.PendingReview)
            .getResultList().stream().findFirst().map(ChangeRequestEntity::toDomain);
    }
}
