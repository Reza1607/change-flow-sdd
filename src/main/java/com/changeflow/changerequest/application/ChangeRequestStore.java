package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequest;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary; implementations participate in the caller's transaction and never retry. */
public interface ChangeRequestStore {
    /** Insert a new request with version 0. Does not append a status history entry. */
    ChangeRequest create(ChangeRequest request);
    Optional<ChangeRequest> find(UUID id);
    /**
     * Compare-and-set: expectedVersion is required and nonnegative. Update only the matching
     * persisted version; increment even for an equal-value edit. Stale/missing row is a conflict,
     * never a blind overwrite. Must share the transaction with AuditTrail.append.
     */
    ChangeRequest update(ChangeRequest request,long expectedVersion);
    /** Version-checked deletion only; bulk deletion without the expected version is forbidden. */
    void delete(UUID id,long expectedVersion);
}
