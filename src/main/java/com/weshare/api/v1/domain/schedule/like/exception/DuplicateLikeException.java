package com.weshare.api.v1.domain.schedule.like.exception;

public class DuplicateLikeException extends RuntimeException {
    public DuplicateLikeException() {
    }

    public DuplicateLikeException(String message) {
        super(message);
    }
}
