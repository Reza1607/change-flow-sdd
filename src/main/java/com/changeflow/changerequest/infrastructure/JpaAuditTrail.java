package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.AuditTrail;
import com.changeflow.changerequest.domain.StatusHistoryEntry;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class JpaAuditTrail implements AuditTrail {
    private final EntityManager em;
    public JpaAuditTrail(EntityManager em) {this.em=em;}
    public void append(StatusHistoryEntry entry) {
        em.persist(StatusHistoryEntity.from(entry));em.flush();
    }
}
