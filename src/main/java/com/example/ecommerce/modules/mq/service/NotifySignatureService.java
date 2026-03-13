package com.example.ecommerce.modules.mq.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class NotifySignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secretBytes;

    public NotifySignatureService(@Value("${app.notify.sign-secret:replace-notify-sign-secret}") String signSecret) {
        this.secretBytes = signSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String timestamp, String notifyId, Long orderId, String transactionId, String sentAt) {
        String payload = canonicalPayload(timestamp, notifyId, orderId, transactionId, sentAt);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("不支持的签名算法", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("签名计算失败", ex);
        }
    }

    public boolean verify(
            String timestamp,
            String notifyId,
            Long orderId,
            String transactionId,
            String sentAt,
            String signature
    ) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = sign(timestamp, notifyId, orderId, transactionId, sentAt);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    private String canonicalPayload(String timestamp, String notifyId, Long orderId, String transactionId, String sentAt) {
        return String.join(
                "\n",
                nullToEmpty(timestamp),
                nullToEmpty(notifyId),
                orderId == null ? "" : orderId.toString(),
                nullToEmpty(transactionId),
                nullToEmpty(sentAt)
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
