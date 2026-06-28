package com.example.eventboard.exception;

public class NoSeatsAvailableException extends RuntimeException {
    public NoSeatsAvailableException(long eventId) {
        super("No seats available for event: " + eventId);
    }
}