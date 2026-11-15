package com.changeflow.changerequest.domain;

import java.util.*;

/** Immutable request. Persistence alone advances the technical version. */
public record ChangeRequest(UUID id,String title,String description,String reason,
        UUID requesterId,ChangeRequestStatus status,long version) {
    public static ChangeRequest draft(UUID id,UUID actor,String title,String description,String reason) {
        return new ChangeRequest(Objects.requireNonNull(id),title,description,reason,Objects.requireNonNull(actor),ChangeRequestStatus.Draft,0);
    }
    public void requireEditableBy(UUID actor) {
        requireOwner(actor);
        if(status!=ChangeRequestStatus.Draft && status!=ChangeRequestStatus.ChangesRequested)
            throw new DraftRuleViolation(DraftRuleViolation.Code.INVALID_TRANSITION,List.of());
    }
    public void requireSubmittableBy(UUID actor) {requireEditableBy(actor);}
    public ChangeRequest edit(UUID actor,String title,String description,String reason) {
        requireEditableBy(actor);
        return new ChangeRequest(id,title,description,reason,requesterId,status,version);
    }
    public ChangeRequest submit(UUID actor) {
        requireSubmittableBy(actor);
        var fields=new ArrayList<String>();
        if(blank(title)) fields.add("title");
        if(blank(description)) fields.add("description");
        if(blank(reason)) fields.add("reason");
        if(!fields.isEmpty()) throw new DraftRuleViolation(DraftRuleViolation.Code.VALIDATION_FAILED,fields);
        return new ChangeRequest(id,title,description,reason,requesterId,ChangeRequestStatus.PendingReview,version);
    }
    private void requireOwner(UUID actor) {
        if(!requesterId.equals(actor)) throw new DraftRuleViolation(DraftRuleViolation.Code.FORBIDDEN,List.of());
    }
    private static boolean blank(String value) {
        return value==null || value.codePoints().allMatch(cp->Character.isWhitespace(cp)||Character.isSpaceChar(cp));
    }
}
