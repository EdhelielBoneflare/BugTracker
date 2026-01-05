package uni.bugtracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uni.bugtracker.backend.utility.ai_criticality.ReportCreatedEvent;

@Service
@RequiredArgsConstructor
public class ReportCriticalityListener {
    private final CriticalityAnalysisService analysisService;

    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @EventListener
    public void handle(ReportCreatedEvent event) {
        analysisService.analyzeAndUpdate(event.reportId());
    }
}
