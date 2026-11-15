package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.HttpIntegrationSupport;
import com.changeflow.changerequest.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class EntityMappingIT extends HttpIntegrationSupport {
    @PersistenceContext EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Test
    void allStatusesNullableTextsAndAuditRoundTripAgainstValidatedSchema() {
        var actor=account(true,true);
        var tx=new TransactionTemplate(manager);
        for(var status:ChangeRequestStatus.values()) {
            var model=new ChangeRequest(UUID.randomUUID(),null,"شرح فارسی",null,actor.id(),status,0);
            tx.executeWithoutResult(s->{
                var entity=ChangeRequestEntity.from(model);
                assertEquals(model,entity.toDomain(),"request mapping must preserve all fields");
                em.persist(entity);em.flush();em.clear();
                assertEquals(model,em.find(ChangeRequestEntity.class,model.id()).toDomain());
            });
        }
        var request=new ChangeRequest(UUID.randomUUID(),null,null,null,actor.id(),ChangeRequestStatus.PendingReview,0);
        var time=Instant.parse("2026-11-15T00:00:00.123456Z");
        var history=new StatusHistoryEntry(UUID.randomUUID(),request.id(),actor.id(),time,
                ChangeRequestStatus.Draft,ChangeRequestStatus.PendingReview,"",1);
        tx.executeWithoutResult(s->{
            var entity=StatusHistoryEntity.from(history);
            assertEquals(history,entity.toDomain(),"audit mapping must preserve all fields");
            em.persist(ChangeRequestEntity.from(request));em.persist(entity);em.flush();em.clear();
            assertEquals(history,em.find(StatusHistoryEntity.class,history.id()).toDomain());
        });
    }
}
