package com.mediaconverter.exception;

public class LogicException extends RuntimeException {

    public LogicException() {
        super();
    }

    public LogicException(String message) {
        super(message);
    }

    public LogicException(String message, Exception e) {
        super(message, e);
    }

}
