package uni.bugtracker.backend.exception;

import org.springframework.http.HttpStatus;

public class ProjectCreationError extends BaseCustomerException {
  public ProjectCreationError(String message) {
    super("PROJECT_CREATION_ERROR", message, HttpStatus.BAD_REQUEST);
  }
}
