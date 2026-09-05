package com.example.jobapi.service;

public class JobClosedException extends RuntimeException {
    public JobClosedException() {
        super("This job is closed and no longer accepts applications");
    }
}
