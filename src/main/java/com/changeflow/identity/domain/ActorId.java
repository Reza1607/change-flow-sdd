package com.changeflow.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record ActorId(UUID value) {
    public ActorId { Objects.requireNonNull(value, "actor id"); }
}
