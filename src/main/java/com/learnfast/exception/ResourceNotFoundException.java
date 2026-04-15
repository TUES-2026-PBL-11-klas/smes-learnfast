package com.learnfast.exception;

public class ResourceNotFoundException extends LearnFastException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found");
    }
}
