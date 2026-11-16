package com.changeflow.changerequest.api;

import com.changeflow.changerequest.application.GetRequestService;
import com.changeflow.changerequest.application.ListRequestsService;
import com.changeflow.changerequest.domain.ChangeRequestStatus;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
public class RequestReadController {
    private final GetRequestService requests;
    private final ListRequestsService lists;
    public RequestReadController(GetRequestService requests,ListRequestsService lists) {this.requests=requests;this.lists=lists;}
    @GetMapping
    public ChangeRequestDtos.Page list(@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) ChangeRequestStatus status) {
        return ChangeRequestDtos.Page.from(lists.list(status,page,size));
    }
    @GetMapping("/{id}")
    public ChangeRequestDtos.Response get(@PathVariable UUID id) {return ChangeRequestDtos.Response.from(requests.get(id));}
}
