package com.changeflow.identity.application;

import com.changeflow.identity.domain.ActorId;

/** Authenticated identity only; caller-controlled fields are never consulted. */
public interface CurrentActor {
    ActorId require();
}
