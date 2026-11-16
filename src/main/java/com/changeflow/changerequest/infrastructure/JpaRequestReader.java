package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.RequestReader;
import com.changeflow.changerequest.domain.*;
import com.changeflow.identity.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JpaRequestReader implements RequestReader {
    private final EntityManager em;
    public JpaRequestReader(EntityManager em) {this.em=em;}
    private static final String VISIBLE="""
        ((:requester=true and r.requesterId=:actor) or (:reviewer=true and r.status=:pending))
        """;
    public Optional<ChangeRequest> findVisible(UUID id,AccessSnapshot access) {
        return bind(em.createQuery("select r from ChangeRequestEntity r where r.id=:id and "+VISIBLE,ChangeRequestEntity.class),access)
            .setParameter("id",id).getResultList().stream().findFirst().map(ChangeRequestEntity::toDomain);
    }
    public Page listVisible(AccessSnapshot access,ChangeRequestStatus status,int page,int size) {
        var where=VISIBLE+(status==null?"":" and r.status=:status");
        var countQuery=bind(em.createQuery("select count(r) from ChangeRequestEntity r where "+where,Long.class),access);
        if(status!=null) countQuery.setParameter("status",status);
        long count=countQuery.getSingleResult();
        long offset=(long)page*size;
        if(offset>=count) return new Page(List.of(),page,size,count,(count+size-1)/size);
        var query=bind(em.createQuery("select r from ChangeRequestEntity r where "+where+" order by r.id asc",ChangeRequestEntity.class),access);
        if(status!=null) query.setParameter("status",status);
        var items=query.setFirstResult(Math.toIntExact(offset)).setMaxResults(size).getResultList().stream().map(ChangeRequestEntity::toDomain).toList();
        return new Page(items,page,size,count,(count+size-1)/size);
    }
    private static <T> TypedQuery<T> bind(TypedQuery<T> query,AccessSnapshot access) {
        return query.setParameter("actor",access.actorId().value())
            .setParameter("requester",access.has(Role.Requester)).setParameter("reviewer",access.has(Role.Reviewer))
            .setParameter("pending",ChangeRequestStatus.PendingReview);
    }
}
