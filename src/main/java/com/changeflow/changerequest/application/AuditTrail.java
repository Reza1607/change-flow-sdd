package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.StatusHistoryEntry;

/** Append-only, in the caller's mutation transaction. Failure MUST roll back both writes. */
public interface AuditTrail {
    /** Exactly one entry per successful transition, with the resulting request version. No independent commit. */
    void append(StatusHistoryEntry entry);
}
