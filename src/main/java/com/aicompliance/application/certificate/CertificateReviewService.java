package com.aicompliance.application.certificate;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CertificateRepository;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.domain.certificate.Certificate;
import com.aicompliance.domain.certificate.CertificateStatus;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "Compliance Review" and "Approval" steps of the Upload -> AI Extract -> Compliance
 * Review -> Approval -> Reminder -> Renewal -> Archive workflow.
 */
@Service
public class CertificateReviewService {

    private final CertificateRepository certificateRepository;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;

    public CertificateReviewService(CertificateRepository certificateRepository,
            CompanyContextProvider companyContextProvider, AuditLogRecorder auditLogRecorder) {
        this.certificateRepository = certificateRepository;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
    }

    @Transactional
    public Certificate approve(UUID certificateId) {
        Certificate certificate = getReviewableCertificate(certificateId);
        certificate.setStatus(CertificateStatus.APPROVED);
        certificate.setReviewedByUserId(companyContextProvider.getCurrentUserId());
        certificate.setReviewedAt(Instant.now());
        certificate = certificateRepository.save(certificate);
        auditLogRecorder.record("CERTIFICATE_APPROVED", "Certificate", certificate.getId());
        return certificate;
    }

    @Transactional
    public Certificate reject(UUID certificateId, String reason) {
        Certificate certificate = getReviewableCertificate(certificateId);
        certificate.setStatus(CertificateStatus.REJECTED);
        certificate.setReviewedByUserId(companyContextProvider.getCurrentUserId());
        certificate.setReviewedAt(Instant.now());
        certificate = certificateRepository.save(certificate);
        auditLogRecorder.record("CERTIFICATE_REJECTED", "Certificate", certificate.getId());
        return certificate;
    }

    private Certificate getReviewableCertificate(UUID certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate", certificateId));
        certificate = companyContextProvider.requireOwnership(certificate, "Certificate", certificateId);

        if (certificate.getStatus() != CertificateStatus.PENDING_REVIEW) {
            throw new InvalidStateException(
                    "Certificate is not pending review (current status: " + certificate.getStatus() + ")");
        }
        if (certificate.getCategoryId() == null || certificate.getCertificateName() == null) {
            throw new InvalidStateException(
                    "Certificate is missing required fields (category/name) - AI extraction may still be running "
                            + "or failed; complete or correct the fields before review");
        }
        return certificate;
    }
}
