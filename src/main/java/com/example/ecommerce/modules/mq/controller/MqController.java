package com.example.ecommerce.modules.mq.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.modules.mq.dto.AsyncProcessLogResponse;
import com.example.ecommerce.modules.mq.dto.DeadLetterResponse;
import com.example.ecommerce.modules.mq.service.AsyncEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mq")
public class MqController {

    private final AsyncEventService asyncEventBus;

    public MqController(AsyncEventService asyncEventBus) {
        this.asyncEventBus = asyncEventBus;
    }

    @GetMapping("/dead-letters")
    public ApiResponse<List<DeadLetterResponse>> deadLetters() {
        return ApiResponse.ok(asyncEventBus.listDeadLetters().stream().map(DeadLetterResponse::from).toList());
    }

    @PostMapping("/dead-letters/{eventId}/retry")
    public ApiResponse<DeadLetterResponse> retry(@PathVariable String eventId) {
        return ApiResponse.ok(DeadLetterResponse.from(asyncEventBus.retryDeadLetter(eventId)));
    }

    @GetMapping("/process-logs")
    public ApiResponse<List<AsyncProcessLogResponse>> processLogs(
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(asyncEventBus.listProcessLogs(limit).stream().map(AsyncProcessLogResponse::from).toList());
    }
}
