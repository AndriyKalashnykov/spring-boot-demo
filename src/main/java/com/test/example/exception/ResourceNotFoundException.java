package com.test.example.exception;

import static java.text.MessageFormat.format;

/**
 * For HTTP 404 errros
 */
public class ResourceNotFoundException extends RuntimeException {
    private long id;

    @Override
    public String getMessage() {
        return format("Hotel id ''{0}'' was not found", id);
    }

    public ResourceNotFoundException(long id) {
        super();
        this.id = id;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }

}
