package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.HistoryReader;
import com.changeflow.changerequest.domain.ChangeRequestStatus;
import com.changeflow.identity.domain.*;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JpaHistoryReader implements HistoryReader {
    private static final String VISIBLE="""
        r.id=:id and ((:requester=true and r.requesterId=:actor)
        or (:reviewer=true and r.status=:pending))
        """;
    private static final String JOIN="from StatusHistoryEntity h join ChangeRequestEntity r on h.requestId=r.id where ";
    private final EntityManager em;
    public JpaHistoryReader(EntityManager em) {this.em=em;}
    public Optional<Page> findVisible(UUID id,AccessSnapshot access,int page,int size) {
        if(bind(em.createQuery("select count(r) from ChangeRequestEntity r where "+VISIBLE,Long.class),id,access).getSingleResult()==0)
            return Optional.empty();
        long count=bind(em.createQuery("select count(h) "+JOIN+VISIBLE,Long.class),id,access).getSingleResult();
        long offset=(long)page*size;
        var items=offset>=count?List.<com.changeflow.changerequest.domain.StatusHistoryEntry>of():
            bind(em.createQuery("select h "+JOIN+VISIBLE+" order by h.resultingVersion asc",StatusHistoryEntity.class),id,access)
                .setFirstResult(Math.toIntExact(offset)).setMaxResults(size).getResultList().stream().map(StatusHistoryEntity::toDomain).toList();
        return Optional.of(new Page(items,page,size,count,(count+size-1)/size));
    }
    private static <T> TypedQuery<T> bind(TypedQuery<T> query,UUID id,AccessSnapshot access) {
        return query.setParameter("id",id).setParameter("actor",access.actorId().value())
            .setParameter("requester",access.has(Role.Requester)).setParameter("reviewer",access.has(Role.Reviewer))
            .setParameter("pending",ChangeRequestStatus.PendingReview);
    }
}
