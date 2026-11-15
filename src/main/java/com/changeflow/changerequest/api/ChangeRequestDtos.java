package com.changeflow.changerequest.api;

import com.changeflow.changerequest.domain.ChangeRequest;
import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

public final class ChangeRequestDtos {
    private ChangeRequestDtos() {}
    public record DraftInput(String title,String description,String reason) {}
    public record EditInput(@NotNull @PositiveOrZero Long expectedVersion,
        @JsonProperty(required=true) String title,@JsonProperty(required=true) String description,@JsonProperty(required=true) String reason) {}
    public record TransitionInput(@NotNull @PositiveOrZero Long expectedVersion,tools.jackson.databind.JsonNode explanation) {
        public TransitionInput {
            // Jackson distinguishes an absent node (Java null) from explicit JSON null (NullNode).
            if(explanation!=null && !explanation.isString()) throw new IllegalArgumentException("explanation must be a string when present");
        }
        public String explanationText() {return explanation==null?null:explanation.asString();}
    }
    public record Response(UUID id,String title,String description,String reason,UUID requesterId,String status,long version) {
        public static Response from(ChangeRequest request) {
            return new Response(request.id(),request.title(),request.description(),request.reason(),request.requesterId(),request.status().name(),request.version());
        }
    }
}
