package com.changeflow.changerequest.api;

import com.changeflow.changerequest.application.HistoryReader;
import com.changeflow.changerequest.domain.StatusHistoryEntry;
import java.time.Instant;
import java.util.*;

public final class HistoryDtos {
    private HistoryDtos() {}
    public record Entry(UUID id,UUID requestId,UUID actorId,Instant occurredAt,String fromStatus,String toStatus,String explanation,long resultingVersion) {
        static Entry from(StatusHistoryEntry entry) {
            return new Entry(entry.id(),entry.requestId(),entry.actorId(),entry.occurredAt(),entry.fromStatus().name(),entry.toStatus().name(),entry.explanation(),entry.resultingVersion());
        }
    }
    public record Page(List<Entry> items,int page,int size,long totalElements,long totalPages) {
        static Page from(HistoryReader.Page result) {
            return new Page(result.items().stream().map(Entry::from).toList(),result.page(),result.size(),result.totalElements(),result.totalPages());
        }
    }
}
