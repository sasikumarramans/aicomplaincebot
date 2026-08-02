package com.aicompliance.infrastructure.ai;

public class OpenAiRequestException extends RuntimeException {

    public OpenAiRequestException(String message) {
        super(message);
    }

    public OpenAiRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
