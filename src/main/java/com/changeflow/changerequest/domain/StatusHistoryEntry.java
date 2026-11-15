package com.changeflow.changerequest.domain;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryEntry(UUID id,UUID requestId,UUID actorId,Instant occurredAt,
        ChangeRequestStatus fromStatus,ChangeRequestStatus toStatus,String explanation,long resultingVersion) {}
