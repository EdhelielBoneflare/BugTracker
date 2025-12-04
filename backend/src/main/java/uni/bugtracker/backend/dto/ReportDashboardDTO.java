package uni.bugtracker.backend.dto;


import lombok.Getter;
import uni.bugtracker.backend.model.Report;

import java.time.Instant;
import java.util.List;

@Getter
public class ReportDashboardDTO {
    private Long id;
    private Long projectId;
    private String title;
    private List<String> tags;
    private Instant date;
    private String level;
    private String status;

    public ReportDashboardDTO(Report report) {
        this.id = report.getId();
        this.projectId = report.getProject().getId();
        this.title = report.getTitle();
        this.tags = report.getTags().stream()
                .map(Enum::name) // or (Tag::toString)
                .toList();
        this.date = report.getDate();
        this.level = report.getCriticality() != null
                ? report.getCriticality().name()
                : null;
        this.status = report.getStatus()!= null
                ? report.getStatus().name()
                : null;
    }
}
