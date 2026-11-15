package com.changeflow.changerequest.application;

import java.util.UUID;

/** A yes/no receipt lookup; never grants access to request or history data. */
public interface ApprovalHistory {
    boolean hasApproval(UUID requestId,UUID actorId,long resultingVersion);
}
