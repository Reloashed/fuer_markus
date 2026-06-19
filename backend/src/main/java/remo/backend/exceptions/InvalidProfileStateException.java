package remo.backend.exceptions;

public class InvalidProfileStateException extends RuntimeException {
    public InvalidProfileStateException(String message) {
        super(message);
    }
}
