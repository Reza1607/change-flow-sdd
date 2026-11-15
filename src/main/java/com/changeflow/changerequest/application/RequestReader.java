package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.ChangeRequest;
import com.changeflow.identity.domain.AccessSnapshot;
import java.util.Optional;
import java.util.UUID;

/** Restrict visibility in the query, before any request data is returned. */
public interface RequestReader {
    Optional<ChangeRequest> findVisible(UUID id,AccessSnapshot access);
}
