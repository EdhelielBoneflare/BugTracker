package uni.bugtracker.backend.dto.report;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

// only for creation on widget

@Data
public class ReportCreationRequestWidget {
    @NotNull
    @NotBlank
    private String projectId;

    @NotNull
    @Positive
    private Long sessionId;

    @NotBlank(message = "Report title cannot be empty")
    @Size(max = 255, message = "To long report title, max={max}")
    private String title;

    @NotNull
    @Size(max = 10, message = "Too many tags, max={max}")
    private List<String> tags;

    @NotNull
    private Instant reportedAt;

    @Size(max = 5000, message = "Too long comments, max={max}")
    private String comments;

    @Email
    private String userEmail;

    // Screenshot may be omitted if user denies consent; make field optional
//    private String screen;

    @NotBlank
    private String currentUrl;

    @NotNull
    private Boolean userProvided;

}
