package com.changeflow.identity.application;

import com.changeflow.identity.domain.AccessSnapshot;
import com.changeflow.identity.domain.ActorId;

public interface CurrentAccess {
    /** Fresh authoritative read; authentication authorities are not authorization. Fails closed. */
    AccessSnapshot read(ActorId actorId);
    /**
     * Re-read under a shared row guard, in the caller's READ COMMITTED transaction.
     * Guard remains until transaction completion (including reads). Caller checks required role
     * on the returned snapshot before accepting work. Missing/disabled/unknown access fails closed.
     */
    AccessSnapshot guard(ActorId actorId);
}
