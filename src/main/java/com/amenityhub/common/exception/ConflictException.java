package com.amenityhub.common.exception;

/**
 * Thrown on concurrency / uniqueness conflicts (e.g. a slot was taken by a
 * concurrent request, or a duplicate registration).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
