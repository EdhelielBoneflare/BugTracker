package uni.bugtracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ReportRequestWidget {
    @NotNull
    @Positive
    private Long projectId;

    @NotBlank
    @Size(max = 255, message = "To long report title, max={max}")
    private String title;

    @NotNull
    @Size(max = 10, message = "Too many tags, max={max}")
    private List<String> tags;

    @NotNull
    private Instant date;

    @Size(max = 5000, message = "Too long comments, max={max}")
    private String comments;

    @NotBlank
    private String log;

    @NotBlank
    private String actions;

    @NotBlank
    private String screen;
}


