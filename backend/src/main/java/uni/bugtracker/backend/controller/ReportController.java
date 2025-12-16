package uni.bugtracker.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.dto.report.ReportCardDTO;
import uni.bugtracker.backend.dto.report.ReportDashboardDTO;
import uni.bugtracker.backend.dto.report.ReportUpdateRequestDashboard;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.service.ReportService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // only for widget
    @PostMapping("/widget")
    public ResponseEntity<?> create(@Valid @RequestBody ReportCreationRequestWidget request) {
        return new ResponseEntity<>(Map.of(
                "message", "Report created",
                "reportId", reportService.createReport(request)),
                HttpStatus.CREATED);
    }

    // access only developer
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Partial data to update",
            content = @Content(
                    schema = @Schema(implementation = ReportUpdateRequestDashboard.class)
            )
    )
    @PatchMapping("/{id}/dashboard")
    public ResponseEntity<ReportCardDTO> updateDev(
            @PathVariable Long id,
            @RequestBody Map<String, Object> raw
    ) {
        return new ResponseEntity<>(
                reportService.updateReportFromDashboard(id, raw),
                HttpStatus.OK
        );
    }

    @GetMapping("/byProject/{projectId}")
    public ResponseEntity<Page<ReportDashboardDTO>> getAllByProject(
            @PathVariable Long projectId,
            @PageableDefault(
                    page = 0,
                    size = 30,
                    sort = "reportedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        Page<ReportDashboardDTO> listOfAllOnProject = reportService.getAllReportsOfProject(projectId, pageable);
        if (listOfAllOnProject.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(listOfAllOnProject, responseStatus);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportCardDTO> getReportCard(
        @PathVariable Long reportId
    ) {
        return new ResponseEntity<>(reportService.getReportCard(reportId), HttpStatus.OK);
    }

    @GetMapping("/byProject/{projectId}/solved")
    public ResponseEntity<Page<ReportDashboardDTO>> getAllReportsSolved(
            @PageableDefault(
                    page = 0,
                    size = 30,
                    sort = "reportedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @PathVariable Long projectId
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        Page<ReportDashboardDTO> listOfSolved = reportService.getAllReportsSolvedOnProject(projectId, pageable);
        if (listOfSolved.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(listOfSolved, responseStatus);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        ReportCardDTO deletedReport = reportService.deleteReport(id);
        return new ResponseEntity<>(deletedReport, HttpStatus.OK);
    }
}
