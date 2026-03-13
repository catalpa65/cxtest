package com.example.ecommerce.modules.mq.model;

public enum AsyncProcessStatus {
    PUBLISHED,
    SUCCESS,
    RETRY_SCHEDULED,
    DEAD_LETTER,
    MANUAL_RETRY
}
