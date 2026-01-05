package uni.bugtracker.backend.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseCustomerException {
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
