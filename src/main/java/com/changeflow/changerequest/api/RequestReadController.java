package com.changeflow.changerequest.api;

import com.changeflow.changerequest.application.GetRequestService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
public class RequestReadController {
    private final GetRequestService requests;
    public RequestReadController(GetRequestService requests) {this.requests=requests;}
    @GetMapping("/{id}")
    public ChangeRequestDtos.Response get(@PathVariable UUID id) {return ChangeRequestDtos.Response.from(requests.get(id));}
}
