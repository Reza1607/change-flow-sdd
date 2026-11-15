package com.changeflow.changerequest.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class DraftRulesTest {
    private final UUID owner=UUID.randomUUID();
    private ChangeRequest request(ChangeRequestStatus status) {
        return new ChangeRequest(UUID.randomUUID(),"عنوان","شرح","دلیل",owner,status,7);
    }
    @Test void createsIncompleteDraftWithoutDeduplicatingOrChangingIdentity() {
        var first=ChangeRequest.draft(UUID.randomUUID(),owner,null,null,null);
        assertNotNull(first);assertEquals(ChangeRequestStatus.Draft,first.status());assertEquals(owner,first.requesterId());assertEquals(0,first.version());
        assertNull(first.title());assertNull(first.description());assertNull(first.reason());
        var second=ChangeRequest.draft(UUID.randomUUID(),owner,null,null,null);
        assertNotEquals(first.id(),second.id());
    }
    @ParameterizedTest @EnumSource(value=ChangeRequestStatus.class,names={"Draft","ChangesRequested"})
    void editsOwnEditableStateAndKeepsIdentityAndVersion(ChangeRequestStatus status) {
        var original=request(status);var edited=original.edit(owner,null,"جدید",null);
        assertEquals(original.id(),edited.id());assertEquals(owner,edited.requesterId());
        assertNull(edited.title());assertNull(edited.reason());assertEquals("جدید",edited.description());
        assertEquals(status,edited.status());assertEquals(7,edited.version());assertEquals("عنوان",original.title());
    }
    @ParameterizedTest @EnumSource(value=ChangeRequestStatus.class,names={"PendingReview","Approved"})
    void rejectsEditingAndSubmittingLockedStates(ChangeRequestStatus status) {
        var original=request(status);
        assertEquals(DraftRuleViolation.Code.INVALID_TRANSITION,assertThrows(DraftRuleViolation.class,()->original.edit(owner,null,null,null)).code());
        assertEquals(DraftRuleViolation.Code.INVALID_TRANSITION,assertThrows(DraftRuleViolation.class,()->original.submit(owner)).code());
        assertEquals(status,original.status());assertEquals(7,original.version());
    }
    @ParameterizedTest @EnumSource(ChangeRequestStatus.class)
    void nonOwnerCannotEditOrSubmitEvenWithCompleteContent(ChangeRequestStatus status) {
        var original=request(status);var other=UUID.randomUUID();
        assertEquals(DraftRuleViolation.Code.FORBIDDEN,assertThrows(DraftRuleViolation.class,()->original.edit(other,"x","y","z")).code());
        assertEquals(DraftRuleViolation.Code.FORBIDDEN,assertThrows(DraftRuleViolation.class,()->original.submit(other)).code());
    }
    static Stream<org.junit.jupiter.params.provider.Arguments> blankFields() {
        var whitespace=new StringBuilder();
        for(int cp=0;cp<=Character.MAX_CODE_POINT;cp++) if(Character.isWhitespace(cp)||Character.isSpaceChar(cp)) whitespace.appendCodePoint(cp);
        return Stream.of("title","description","reason").flatMap(field->Stream.of(null,""," \t\n", "\u00a0\u2007\u202f",whitespace.toString())
            .map(text->org.junit.jupiter.params.provider.Arguments.of(field,text)));
    }
    @ParameterizedTest @MethodSource("blankFields")
    void invalidSubmissionNamesFieldAndLeavesOriginalUnchanged(String field,String text) {
        var original=new ChangeRequest(UUID.randomUUID(),field.equals("title")?text:"t",field.equals("description")?text:"d",
            field.equals("reason")?text:"r",owner,ChangeRequestStatus.Draft,0);
        var failure=assertThrows(DraftRuleViolation.class,()->original.submit(owner));
        assertEquals(DraftRuleViolation.Code.VALIDATION_FAILED,failure.code());assertEquals(List.of(field),failure.fields());
        assertEquals(ChangeRequestStatus.Draft,original.status());assertEquals(0,original.version());
    }
    @Test void validSubmitPreservesContentAndMovesToPendingWithoutMutatingOriginal() {
        var original=request(ChangeRequestStatus.Draft);var submitted=original.submit(owner);
        assertEquals(ChangeRequestStatus.PendingReview,submitted.status());assertEquals(original.id(),submitted.id());
        assertEquals(owner,submitted.requesterId());assertEquals(original.title(),submitted.title());
        assertEquals(original.version(),submitted.version());assertEquals(ChangeRequestStatus.Draft,original.status());
    }
}
