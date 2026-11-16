package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequest;
import com.changeflow.identity.domain.AccessSnapshot;
import java.util.Optional;
import java.util.List;
import com.changeflow.changerequest.domain.ChangeRequestStatus;
import java.util.UUID;

/** Restrict visibility in the query, before any request data is returned. */
public interface RequestReader {
    record Page(List<ChangeRequest> items,int page,int size,long totalElements,long totalPages) {
        public Page {items=List.copyOf(items);}
    }
    Page listVisible(AccessSnapshot access,ChangeRequestStatus status,int page,int size);
    Optional<ChangeRequest> findVisible(UUID id,AccessSnapshot access);
}
