package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepeatedSubmitIT extends DraftApiSupport {
    @Test void repeatPrecedesStaleVersionAndLeavesFullRowAndHistoryUnchanged() throws Exception {
        var owner=account(true,false);var other=account(true,true);
        var draft=create(owner,"{\"title\":\"t\",\"description\":\"d\",\"reason\":\"r\"}");var id=UUID.fromString(draft.get("id").asString());
        assertEquals(200,mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}").statusCode());
        var before=snapshot(id);var history=jdbc.queryForList("select * from status_history where request_id=?",id);assertEquals(1,history.size());
        for(long version:new long[]{0,1}) problem(mutate(owner,"POST",BASE+"/"+id+"/submit",encode(Map.of("expectedVersion",version))),409,"INVALID_TRANSITION");
        problem(mutate(other,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":1}"),403,"FORBIDDEN");
        assertEquals(before,snapshot(id));assertEquals(history,jdbc.queryForList("select * from status_history where request_id=?",id));
    }
}
