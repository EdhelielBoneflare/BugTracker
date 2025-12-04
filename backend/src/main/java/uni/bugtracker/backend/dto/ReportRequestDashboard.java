package uni.bugtracker.backend.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

// only for updates on dashboard

@Data
public class ReportRequestDashboard {
    @Positive
    private Long projectId;

    @Size(min=1, max = 255, message = "Too long report title, max={max}. Or you entered empty title")
    private String title;

    @Size(max = 10, message = "Too many tags, max={max}")
    private List<String> tags;

    private Instant date;

    @Size(min=1, max = 5000, message = "Too long comments, max={max}. Or you entered empty comments")
    private String comments;

    private String developerName;

    private String level;

    private String status;
}
