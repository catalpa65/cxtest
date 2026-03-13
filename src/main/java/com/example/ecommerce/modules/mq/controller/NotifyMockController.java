package com.example.ecommerce.modules.mq.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.mq.service.NotifyReplayGuard;
import com.example.ecommerce.modules.mq.service.NotifySignatureService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notify")
public class NotifyMockController {

    private final NotifySignatureService notifySignatureService;
    private final NotifyReplayGuard notifyReplayGuard;

    public NotifyMockController(
            NotifySignatureService notifySignatureService,
            NotifyReplayGuard notifyReplayGuard
    ) {
        this.notifySignatureService = notifySignatureService;
        this.notifyReplayGuard = notifyReplayGuard;
    }

    @PostMapping("/order-paid")
    public ApiResponse<NotifyAckResponse> orderPaid(
            @RequestHeader(name = "X-Notify-Timestamp") String timestamp,
            @RequestHeader(name = "X-Notify-Id") String notifyId,
            @RequestHeader(name = "X-Notify-Signature") String signature,
            @RequestBody @Valid NotifyOrderPaidRequest request
    ) {
        java.time.Instant parsedTimestamp;
        try {
            parsedTimestamp = java.time.Instant.parse(timestamp);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "通知时间戳格式非法");
        }
        notifyReplayGuard.checkAndRecord(notifyId, parsedTimestamp);

        boolean valid = notifySignatureService.verify(
                timestamp,
                notifyId,
                request.orderId(),
                request.transactionId(),
                request.sentAt(),
                signature
        );
        if (!valid) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "通知签名非法");
        }
        return ApiResponse.ok(new NotifyAckResponse(true, request.orderId(), request.transactionId()));
    }

    public record NotifyOrderPaidRequest(
            @NotNull Long orderId,
            @NotBlank String transactionId,
            @NotBlank String sentAt
    ) {
    }

    public record NotifyAckResponse(
            boolean accepted,
            Long orderId,
            String transactionId
    ) {
    }
}
