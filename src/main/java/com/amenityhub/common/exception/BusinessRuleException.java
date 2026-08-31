package com.amenityhub.common.exception;

/**
 * Thrown when a request is syntactically valid but violates a domain rule
 * (e.g. booking a slot that is full, cancelling outside the allowed window).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
