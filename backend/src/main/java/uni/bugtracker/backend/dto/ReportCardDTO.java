package uni.bugtracker.backend.dto;


import lombok.Getter;
import uni.bugtracker.backend.model.Report;

import java.time.Instant;
import java.util.List;

@Getter
public class ReportCardDTO {
    private Long id;
    private Long projectId;
    private String title;
    private List<String> tags;
    private Instant date;
    private String level;
    private String status;
    private String log;
    private String actions;
    private String comments;
    private String screen;
    private String devName;


    public ReportCardDTO(Report report) {
        this.id = report.getId();
        this.projectId = report.getProject().getId();
        this.title = report.getTitle();
        this.tags = report.getTags().stream()
                .map(Enum::name)
                .toList();
        this.date = report.getDate();
        this.level = report.getCriticality() != null
                ? report.getCriticality().name()
                : null;
        this.status = report.getStatus()!= null
                ? report.getStatus().name()
                : null;
        this.log = report.getLog();
        this.actions = report.getActions();
        this.comments = report.getComments();
        this.screen = report.getScreen();
        this.devName = report.getDeveloper()!=null
                ? report.getDeveloper().getUsername()
                : null;
    }
}
