package uni.bugtracker.backend.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.dto.ReportCardDTO;
import uni.bugtracker.backend.dto.ReportDashboardDTO;
import uni.bugtracker.backend.dto.ReportRequestDashboard;
import uni.bugtracker.backend.dto.ReportRequestWidget;
import uni.bugtracker.backend.service.ReportService;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // only for widget
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody ReportRequestWidget request) {
        return new ResponseEntity<>(reportService.createReport(request), HttpStatus.CREATED);
    }

    // for widget
    @PatchMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequestWidget request
    ) {
        return new ResponseEntity<>(reportService.updateReportFromWidget(id, request), HttpStatus.OK);
    }

    // access only developer
    @PatchMapping("/update/dev/{id}")
    public ResponseEntity<?> updateDev(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequestDashboard request
    ) {
        return new ResponseEntity<>(reportService.updateReportFromDashboard(id, request), HttpStatus.OK);
    }

    @GetMapping("/byProject/{projectId}")
    public ResponseEntity<?> getAllByProject(
            @PathVariable Long projectId,
            @PageableDefault(
                    page = 0,
                    size = 30,
                    sort = "date",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        List<ReportDashboardDTO> listOfAllOnProject = reportService.getAllReportsOfProject(projectId, pageable);
        if (listOfAllOnProject.isEmpty()) {
            responseStatus = HttpStatus.NO_CONTENT;
        }
        return new ResponseEntity<>(listOfAllOnProject, responseStatus);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReportCard(
        @PathVariable Long reportId
    ) {
        return new ResponseEntity<>(reportService.getReportCard(reportId), HttpStatus.OK);
    }

    @GetMapping("/solved")
    public ResponseEntity<?> getAllReportsSolved(
            @PageableDefault(
                    page = 0,
                    size = 30,
                    sort = "date",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        HttpStatus responseStatus = HttpStatus.OK;
        List<ReportDashboardDTO> listOfSolved = reportService.getAllReportsSolved(pageable);
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
