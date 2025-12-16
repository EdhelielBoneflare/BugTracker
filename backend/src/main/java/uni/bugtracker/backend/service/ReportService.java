package uni.bugtracker.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.dto.report.ReportCardDTO;
import uni.bugtracker.backend.dto.report.ReportDashboardDTO;
import uni.bugtracker.backend.dto.report.ReportCreationRequestWidget;
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

    private static final int MAX_TAGS = 10;
    private static final int MAX_COMMENTS = 5000;

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
    public ReportCardDTO updateReportFromDashboard(Long id, Map<String, Object> raw) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report doesn't exist"));

        Set<String> fields = raw.keySet();

        Project project = null;
        if (fields.contains("projectId")) {
            Long projectId = getLongValue(raw, "projectId");
            if (projectId == null)
                throw new BusinessValidationException("INVALID_ARGUMENT", "projectId cannot be null");

            project = projectRepository.findById(projectId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Project with id " + projectId + " doesn't exist"));
        }

        Developer developer = null;
        if (fields.contains("developerName")) {
            String developerName = getStringValue(raw, "developerName");
            if (developerName != null) {
                developer = developerRepository
                        .findByUsername(developerName)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Developer '" + developerName + "' doesn't exist"));
            }
        }
        report = mapper.updateFromDashboard(report, raw, fields, project, developer);
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

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}

