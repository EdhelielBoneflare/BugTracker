package uni.bugtracker.backend.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseCustomerException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public BaseCustomerException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}


