package com.changeflow.changerequest.api;

import com.changeflow.changerequest.application.DraftRequestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
public class DraftRequestController {
    private final DraftRequestService drafts;
    public DraftRequestController(DraftRequestService drafts) {this.drafts=drafts;}
    @PostMapping
    public ResponseEntity<ChangeRequestDtos.Response> create(@Valid @RequestBody ChangeRequestDtos.DraftInput input) {
        var response=ChangeRequestDtos.Response.from(drafts.create(input.title(),input.description(),input.reason()));
        return ResponseEntity.created(URI.create("/api/v1/change-requests/"+response.id())).body(response);
    }
    @PutMapping("/{id}")
    public ChangeRequestDtos.Response edit(@PathVariable UUID id,@Valid @RequestBody ChangeRequestDtos.EditInput input) {
        return ChangeRequestDtos.Response.from(drafts.edit(id,input.expectedVersion(),input.title(),input.description(),input.reason()));
    }
    @PostMapping("/{id}/submit")
    public ChangeRequestDtos.Response submit(@PathVariable UUID id,@Valid @RequestBody ChangeRequestDtos.TransitionInput input) {
        return ChangeRequestDtos.Response.from(drafts.submit(id,input.expectedVersion(),input.explanationText()));
    }
}
