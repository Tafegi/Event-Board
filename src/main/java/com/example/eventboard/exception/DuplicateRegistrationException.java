package com.example.eventboard.exception;

public class DuplicateRegistrationException extends RuntimeException {
    public DuplicateRegistrationException(String email) {
        super("Student is already registered for this event");
    }
}