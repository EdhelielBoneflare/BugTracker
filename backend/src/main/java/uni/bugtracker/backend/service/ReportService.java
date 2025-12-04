package uni.bugtracker.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.dto.ReportCardDTO;
import uni.bugtracker.backend.dto.ReportDashboardDTO;
import uni.bugtracker.backend.dto.ReportRequestDashboard;
import uni.bugtracker.backend.dto.ReportRequestWidget;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.model.ReportStatus;
import uni.bugtracker.backend.model.Tag;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.repository.ReportRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;

    private static final int MAX_TAGS = 10;
    private static final int MAX_COMMENTS = 5000;
    private static final int MAX_LOG = 5_000_000;
    private static final int MAX_ACTIONS = 20_000;

    public Report createReport(ReportRequestWidget request) {
        Report entity = new Report();

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NoSuchElementException("Project doesn't exist"));
        entity.setProject(project);
        entity.setTitle(trim(request.getTitle(), 255));
        entity.setDate(request.getDate());

        List<Tag> tags = request.getTags().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> isValidEnum(Tag.class, s))
                .limit(MAX_TAGS)
                .map(Tag::valueOf)
                .toList();
        entity.setTags(tags);

        entity.setComments(trim(request.getComments(), MAX_COMMENTS));
        entity.setLog(trim(request.getLog(), MAX_LOG));
        entity.setActions(trim(request.getActions(), MAX_ACTIONS));
        entity.setScreen(request.getScreen());

        // default values
        entity.setStatus(ReportStatus.NEW);
//        entity.setCriticality(CriticalityLevel.MEDIUM);

        return reportRepository.save(entity);
    }

    @Transactional
    public Report updateReportFromWidget(Long id, ReportRequestWidget request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Report doesn't exist"));
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                            .orElseThrow(() -> new NoSuchElementException("Project doesn't exist"));
            report.setProject(project);
        }
        if (request.getTitle() != null) {
            report.setTitle(trim(request.getTitle(), 255));
        }
        if (request.getDate() != null) {
            report.setDate(request.getDate());
        }
        if (request.getLog() != null) {
            report.setLog(trim(request.getLog(), MAX_LOG));
        }
        if (request.getComments() != null) {
            report.setComments(trim(request.getComments(), MAX_COMMENTS));
        }
        if (request.getActions() != null) {
            report.setActions(trim(request.getActions(), MAX_ACTIONS));
        }
        if (request.getScreen() != null) {
            report.setScreen(request.getScreen());
        }
        if (request.getTags() != null) {
            List<Tag> newTags = request.getTags().stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .limit(MAX_TAGS)
                    .filter(s -> isValidEnum(Tag.class, s))
                    .map(Tag::valueOf)
                    .toList();
            report.setTags(newTags);
        }
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateReportFromDashboard(Long id, ReportRequestDashboard request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Report doesn't exist"));
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new NoSuchElementException("Project doesn't exist"));
            report.setProject(project);
        }
        if (request.getTitle() != null) {
            report.setTitle(trim(request.getTitle(), 255));
        }
        if (request.getDate() != null) {
            report.setDate(request.getDate());
        }
        if (request.getComments() != null) {
            report.setComments(trim(request.getComments(), MAX_COMMENTS));
        }
        if (request.getTags() != null) {
            List<Tag> newTags = request.getTags().stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .limit(MAX_TAGS)
                    .filter(s -> isValidEnum(Tag.class, s))
                    .map(Tag::valueOf)
                    .toList();
            report.setTags(newTags);
        }
        return reportRepository.save(report);
    }

    public Report getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Report doesn't exist"));
    }

    public ReportCardDTO getReportCard(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Report doesn't exist"));
        return new ReportCardDTO(report);
    }

    public List<ReportDashboardDTO> getAllReportsOfProject(Long projectId, Pageable pageable) {
        return reportRepository.findAllByProjectId(projectId, pageable)
                .stream()
                .map(ReportDashboardDTO::new)
                .toList();
    }

    public List<ReportDashboardDTO> getAllReportsSolved(Pageable pageable) {
        return reportRepository.findAllByStatus(ReportStatus.DONE, pageable)
                .stream()
                .map(ReportDashboardDTO::new)
                .toList();
    }

    @Transactional
    public ReportCardDTO deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Report doesn't exist"));
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
}
