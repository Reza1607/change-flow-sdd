package com.changeflow.changerequest.domain;

import java.util.UUID;

/** Foundation data shape only. Creation, editing and transition rules belong to US1 and US2. */
public record ChangeRequest(UUID id,String title,String description,String reason,
        UUID requesterId,ChangeRequestStatus status,long version) {}
