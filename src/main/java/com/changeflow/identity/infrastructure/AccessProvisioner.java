package com.changeflow.identity.infrastructure;

import com.changeflow.identity.domain.ActorId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Controlled internal writer; deliberately has no HTTP endpoint. Commit is the A-006 revocation boundary. */
@Component
public class AccessProvisioner {
    private final JdbcTemplate jdbc;
    public AccessProvisioner(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED)
    public void change(ActorId id,long expectedVersion,boolean enabled,boolean requester,boolean reviewer) {
        var versions=jdbc.query("select access_version from identity_account where actor_id=? FOR UPDATE",(r,n)->r.getLong(1),id.value());
        if(versions.isEmpty()) throw new AccessDeniedException("Access denied");
        if(expectedVersion<0 || versions.getFirst()!=expectedVersion) throw conflict(id);
        int changed=jdbc.update("update identity_account set enabled=?,requester=?,reviewer=?,access_version=access_version+1 where actor_id=? and access_version=?",
                enabled,requester,reviewer,id.value(),expectedVersion);
        if(changed!=1) throw conflict(id);
    }
    private ObjectOptimisticLockingFailureException conflict(ActorId id) {
        return new ObjectOptimisticLockingFailureException(IdentityAccountEntity.class,id.value());
    }
}
