package com.example.patientservice.exception;

public class EmailAlreadyExitsException extends RuntimeException {

    public EmailAlreadyExitsException(String message) {
        super(message);
    }
}
