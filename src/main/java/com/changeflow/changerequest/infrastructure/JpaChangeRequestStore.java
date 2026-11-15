package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.ChangeRequestStore;
import com.changeflow.changerequest.domain.ChangeRequest;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JpaChangeRequestStore implements ChangeRequestStore {
    private final ChangeRequestRepository repository;
    private final EntityManager em;
    public JpaChangeRequestStore(ChangeRequestRepository repository,EntityManager em) {this.repository=repository;this.em=em;}
    public ChangeRequest create(ChangeRequest request) {
        if(request.version()!=0) throw new IllegalArgumentException("New request version must be zero");
        var entity=ChangeRequestEntity.from(request);em.persist(entity);em.flush();return entity.toDomain();
    }
    public Optional<ChangeRequest> find(UUID id) {return repository.findById(id).map(ChangeRequestEntity::toDomain);}
    public ChangeRequest update(ChangeRequest request,long expectedVersion) {
        var entity=versionChecked(request.id(),expectedVersion);
        if(entity.apply(request)) {em.flush();return entity.toDomain();}
        // Standard JPA forces an optimistic version increment even for an equal-value edit.
        // A provider may defer it to commit; the service returns only after that commit succeeds.
        em.lock(entity,LockModeType.OPTIMISTIC_FORCE_INCREMENT);em.flush();
        return new ChangeRequest(request.id(),request.title(),request.description(),request.reason(),request.requesterId(),request.status(),expectedVersion+1);
    }
    public void delete(UUID id,long expectedVersion) {
        em.remove(versionChecked(id,expectedVersion));em.flush();
    }
    private ChangeRequestEntity versionChecked(UUID id,long expectedVersion) {
        var entity=em.find(ChangeRequestEntity.class,id);
        if(expectedVersion<0 || entity==null || entity.toDomain().version()!=expectedVersion)
            throw new ObjectOptimisticLockingFailureException(ChangeRequestEntity.class,id);
        return entity;
    }
}
