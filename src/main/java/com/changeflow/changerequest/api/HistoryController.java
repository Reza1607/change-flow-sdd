package com.changeflow.changerequest.api;

import com.changeflow.changerequest.application.HistoryQueryService;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
public class HistoryController {
    private final HistoryQueryService history;
    public HistoryController(HistoryQueryService history) {this.history=history;}
    @GetMapping("/{id}/history")
    public HistoryDtos.Page list(@PathVariable UUID id,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return HistoryDtos.Page.from(history.list(id,page,size));
    }
}
