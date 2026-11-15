package com.changeflow.identity.infrastructure;

import com.changeflow.identity.application.CurrentAccess;
import com.changeflow.identity.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DatabaseCurrentAccess implements CurrentAccess {
    private final JdbcTemplate jdbc;
    public DatabaseCurrentAccess(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Override @Transactional(readOnly=true,isolation=Isolation.READ_COMMITTED)
    public AccessSnapshot read(ActorId id) {return load(id,"");}
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public AccessSnapshot guard(ActorId id) {
        var isolation=TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        if(isolation!=null && isolation!=java.sql.Connection.TRANSACTION_READ_COMMITTED)
            throw new IllegalStateException("Access guard requires READ COMMITTED");
        // JDBC deliberately avoids an already-managed JPA account or authentication snapshot.
        var snapshot=load(id," FOR SHARE");
        if(!snapshot.enabled()) throw new AccessDeniedException("Access denied");
        return snapshot;
    }
    private AccessSnapshot load(ActorId id,String lock) {
        return jdbc.query("select enabled,requester,reviewer,access_version from identity_account where actor_id=?"+lock,
            (r,n)->new AccessSnapshot(id,r.getBoolean(1),r.getBoolean(2),r.getBoolean(3),r.getLong(4)),id.value())
            .stream().findFirst().orElseThrow(()->new AccessDeniedException("Access denied"));
    }
}
