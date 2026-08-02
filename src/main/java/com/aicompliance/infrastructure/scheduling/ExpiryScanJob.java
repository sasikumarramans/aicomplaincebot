package com.aicompliance.infrastructure.scheduling;

import com.aicompliance.application.expiry.ExpiryBucketingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryScanJob {

    private final ExpiryBucketingService expiryBucketingService;

    public ExpiryScanJob(ExpiryBucketingService expiryBucketingService) {
        this.expiryBucketingService = expiryBucketingService;
    }

    @Scheduled(cron = "${app.expiry-scan.cron:0 0 2 * * *}")
    public void run() {
        expiryBucketingService.recomputeAllBuckets();
    }
}
