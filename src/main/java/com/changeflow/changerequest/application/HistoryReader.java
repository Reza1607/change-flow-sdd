package com.changeflow.changerequest.application;

import com.changeflow.changerequest.domain.StatusHistoryEntry;
import com.changeflow.identity.domain.AccessSnapshot;
import java.util.*;

/** Read history only through the visibility of its parent request. */
public interface HistoryReader {
    record Page(List<StatusHistoryEntry> items,int page,int size,long totalElements,long totalPages) {
        public Page {items=List.copyOf(items);}
    }
    Optional<Page> findVisible(UUID requestId,AccessSnapshot access,int page,int size);
}
