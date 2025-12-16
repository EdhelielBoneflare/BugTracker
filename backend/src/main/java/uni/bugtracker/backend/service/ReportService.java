package uni.bugtracker.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.dto.report.ReportCardDTO;
import uni.bugtracker.backend.dto.report.ReportDashboardDTO;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
import uni.bugtracker.backend.dto.report.ReportUpdateRequestDashboard;
import uni.bugtracker.backend.exception.BusinessValidationException;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.*;
import uni.bugtracker.backend.repository.*;
import uni.bugtracker.backend.utility.ReportMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final SessionRepository sessionRepository;
    private final DeveloperRepository developerRepository;
    private final EventRepository eventRepository;
    private final ReportMapper mapper;

    @Transactional
    public Long createReport(ReportCreationRequestWidget request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project doesn't exist"));
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session doesn't exist"));
        Report report = mapper.fromCreateOnWidget(request, project, session);
        List<Event> events = eventRepository.findAllBySessionId(session.getId());
        mapper.attachEvents(report, events);
        session.setEndTime(
                eventRepository.findFirstBySessionIdOrderByTimestampDesc(session.getId())
                        .map(Event::getTimestamp)
                        .orElse(null)
        );
        return reportRepository.save(report).getId();
    }


    @Transactional
    public ReportCardDTO updateReportFromDashboard(Long id, ReportUpdateRequestDashboard request, String rawJson) {
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = objMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            throw new BusinessValidationException("Invalid JSON", e.toString());
        }
        Set<String> fields = getJsonFields(jsonNode);

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report doesn't exist"));

        Project project = null;
        if (fields.contains("projectId")) {
            if (request.getProjectId() == null)
                throw new BusinessValidationException("INVALID_ARGUMENT", "projectId cannot be null");

            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Project with id " + request.getProjectId() + " doesn't exist"));
        }

        Developer developer = null;
        if (fields.contains("developerName")) {
            if (request.getDeveloperName() != null) {
                developer = developerRepository
                        .findByUsername(request.getDeveloperName())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Developer '" + request.getDeveloperName() + "' doesn't exist"));
            }
        }
        report = mapper.updateFromDashboard(report, request, fields, project, developer);
        return new ReportCardDTO(reportRepository.save(report));
    }

    public Report getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report doesn't exist"));
    }

    public ReportCardDTO getReportCard(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report doesn't exist"));
        return new ReportCardDTO(report);
    }

    @Transactional
    public Page<ReportDashboardDTO> getAllReportsOfProject(Long projectId, Pageable pageable) {
        Page<Report> reportsPage = reportRepository.findAllByProjectId(projectId, pageable);
        return reportsPage.map(ReportDashboardDTO::new);
    }

    public Page<ReportDashboardDTO> getAllReportsSolvedOnProject(Long projectId, Pageable pageable) {
        Page<Report> reportsPage = reportRepository.findAllByProjectIdAndStatus(projectId, ReportStatus.DONE, pageable);
        return reportsPage.map(ReportDashboardDTO::new);
    }

    @Transactional
    public ReportCardDTO deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report doesn't exist"));
        reportRepository.delete(report);
        return new ReportCardDTO(report);
    }

    private Set<String> getJsonFields(JsonNode node) {
        Set<String> fields = new HashSet<>();
        if (node.isObject()) {
//            Iterator<String> fieldNames = node.fieldNames();
//            while (fieldNames.hasNext()) {
//                fields.add(fieldNames.next());
//            }
            node.fieldNames().forEachRemaining(fields::add);
        }
        return fields;
    }

}

