package com.changeflow.changerequest.domain;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.changeflow.changerequest.domain.TransitionPolicy.Command.*;

class TransitionRulesTest {
    private final UUID owner=UUID.randomUUID(),reviewer=UUID.randomUUID();
    private ChangeRequest request(ChangeRequestStatus state) {return new ChangeRequest(UUID.randomUUID(),"t","d","r",owner,state,5);}
    static Stream<Arguments> matrix() {
        return Arrays.stream(ChangeRequestStatus.values()).flatMap(s->Arrays.stream(TransitionPolicy.Command.values()).map(c->Arguments.of(s,c)));
    }
    @ParameterizedTest @MethodSource("matrix")
    void onlyFourTransitionsAreAllowedAndOriginalRemainsUnchanged(ChangeRequestStatus state,TransitionPolicy.Command command) {
        var before=request(state);var actor=command==Submit?owner:reviewer;
        var target=switch(command) {case Submit->ChangeRequestStatus.PendingReview;case Approve->ChangeRequestStatus.Approved;case RequestChanges->ChangeRequestStatus.ChangesRequested;};
        boolean allowed=command==Submit?(state==ChangeRequestStatus.Draft||state==ChangeRequestStatus.ChangesRequested):state==ChangeRequestStatus.PendingReview;
        if(allowed) {
            var after=TransitionPolicy.apply(before,command,actor,true,"reason");
            assertEquals(target,after.status());assertEquals(before.id(),after.id());assertEquals(owner,after.requesterId());
            assertEquals(before.version(),after.version());assertEquals(before.title(),after.title());
        } else assertEquals(DomainFailure.Code.INVALID_TRANSITION,assertThrows(DomainFailure.class,()->TransitionPolicy.apply(before,command,actor,true,"reason")).code());
        assertEquals(state,before.status());assertEquals(5,before.version());
    }
    @ParameterizedTest @EnumSource(value=TransitionPolicy.Command.class,names={"Approve","RequestChanges"})
    void selfReviewIsIdentityBasedEvenWhenActorHasBothRoles(TransitionPolicy.Command command) {
        var before=request(ChangeRequestStatus.PendingReview);
        assertEquals(DomainFailure.Code.SELF_REVIEW_FORBIDDEN,assertThrows(DomainFailure.class,()->TransitionPolicy.apply(before,command,owner,true,"reason")).code());
        assertEquals(ChangeRequestStatus.PendingReview,before.status());
    }
    @ParameterizedTest @EnumSource(TransitionPolicy.Command.class)
    void currentRequiredRoleAndIdentityAreMandatory(TransitionPolicy.Command command) {
        var before=request(command==Submit?ChangeRequestStatus.Draft:ChangeRequestStatus.PendingReview);var actor=command==Submit?owner:reviewer;
        assertEquals(DomainFailure.Code.FORBIDDEN,assertThrows(DomainFailure.class,()->TransitionPolicy.apply(before,command,actor,false,"reason")).code());
        assertEquals(DomainFailure.Code.FORBIDDEN,assertThrows(DomainFailure.class,()->TransitionPolicy.apply(before,command,null,true,"reason")).code());
    }
    @Test void submitByAnotherActorIsForbidden() {
        assertEquals(DomainFailure.Code.FORBIDDEN,assertThrows(DomainFailure.class,()->TransitionPolicy.apply(request(ChangeRequestStatus.Draft),Submit,reviewer,true,null)).code());
    }
    static Stream<Arguments> blankExplanations() {
        var all=new StringBuilder();for(int cp=0;cp<=Character.MAX_CODE_POINT;cp++) if(Character.isWhitespace(cp)||Character.isSpaceChar(cp)) all.appendCodePoint(cp);
        return Stream.of(null,""," \t\n","\u00a0\u2007\u202f",all.toString()).map(Arguments::of);
    }
    @ParameterizedTest @MethodSource("blankExplanations")
    void returnRequiresNonblankExplanation(String explanation) {
        var before=request(ChangeRequestStatus.PendingReview);
        var failure=assertThrows(DomainFailure.class,()->TransitionPolicy.apply(before,RequestChanges,reviewer,true,explanation));
        assertEquals(DomainFailure.Code.VALIDATION_FAILED,failure.code());assertEquals(List.of("explanation"),failure.fields());assertEquals(ChangeRequestStatus.PendingReview,before.status());
    }
    @Test void approvalExplanationIsOptionalAndSubmitStillValidatesAllThreeFields() {
        assertEquals(ChangeRequestStatus.Approved,TransitionPolicy.apply(request(ChangeRequestStatus.PendingReview),Approve,reviewer,true,null).status());
        for(var field:List.of("title","description","reason")) {
            var before=new ChangeRequest(UUID.randomUUID(),field.equals("title")?null:"t",field.equals("description")?"\u00a0":"d",field.equals("reason")?"":"r",owner,ChangeRequestStatus.Draft,0);
            var failure=assertThrows(DomainFailure.class,()->TransitionPolicy.apply(before,Submit,owner,true,null));
            assertEquals(DomainFailure.Code.VALIDATION_FAILED,failure.code());assertEquals(List.of(field),failure.fields());
        }
    }
}
