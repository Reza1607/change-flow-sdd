package com.changeflow.identity.domain;

public record AccessSnapshot(ActorId actorId, boolean enabled, boolean requester, boolean reviewer, long accessVersion) {
    public boolean has(Role role) {
        return enabled && switch(role) { case Requester -> requester; case Reviewer -> reviewer; };
    }
}
