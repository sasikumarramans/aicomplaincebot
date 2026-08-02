package com.aicompliance.infrastructure.scheduling;

import com.aicompliance.application.compliance.RiskAssessmentService;
import com.aicompliance.application.port.CompanyRepository;
import com.aicompliance.domain.company.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessmentJob {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentJob.class);

    private final CompanyRepository companyRepository;
    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentJob(CompanyRepository companyRepository, RiskAssessmentService riskAssessmentService) {
        this.companyRepository = companyRepository;
        this.riskAssessmentService = riskAssessmentService;
    }

    @Scheduled(cron = "${app.risk-assessment.cron:0 30 2 * * *}")
    public void run() {
        int assessed = 0;
        for (Company company : companyRepository.findAll()) {
            try {
                riskAssessmentService.assessForCompany(company.getId());
                assessed++;
            } catch (Exception e) {
                log.error("Risk assessment failed for company {}", company.getId(), e);
            }
        }
        log.info("Risk assessment sweep: {} companies assessed", assessed);
    }
}
