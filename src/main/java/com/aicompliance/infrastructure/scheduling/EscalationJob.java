package com.aicompliance.infrastructure.scheduling;

import com.aicompliance.application.notification.EscalationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EscalationJob {

    private final EscalationService escalationService;

    public EscalationJob(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(cron = "${app.escalation.cron:0 0 * * * *}")
    public void run() {
        escalationService.escalateOverdue();
    }
}
