package uni.bugtracker.backend.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends BaseCustomerException {
    public BusinessValidationException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
