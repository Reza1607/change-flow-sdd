package com.changeflow.changerequest.api;

import com.changeflow.changerequest.application.ReviewRequestService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
public class ReviewRequestController {
    private final ReviewRequestService reviews;
    public ReviewRequestController(ReviewRequestService reviews) {this.reviews=reviews;}
    @PostMapping("/{id}/request-changes")
    public ChangeRequestDtos.Response requestChanges(@PathVariable UUID id,@Valid @RequestBody ChangeRequestDtos.TransitionInput input) {
        return ChangeRequestDtos.Response.from(reviews.requestChanges(id,input.expectedVersion(),input.explanationText()));
    }
    @PostMapping("/{id}/approve")
    public ChangeRequestDtos.Response approve(@PathVariable UUID id,@Valid @RequestBody ChangeRequestDtos.TransitionInput input) {
        return ChangeRequestDtos.Response.from(reviews.approve(id,input.expectedVersion(),input.explanationText()));
    }
}
