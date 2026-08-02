package com.aicompliance.application.company;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.CompanySignupRequestRepository;
import com.aicompliance.application.user.UserService;
import com.aicompliance.domain.company.Company;
import com.aicompliance.domain.company.CompanySignupRequest;
import com.aicompliance.domain.company.SignupRequestStatus;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import com.aicompliance.domain.user.User;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupRequestService {

    private static final Logger log = LoggerFactory.getLogger(SignupRequestService.class);
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 14;

    private final CompanySignupRequestRepository signupRequestRepository;
    private final CompanyService companyService;
    private final UserService userService;
    private final CompanyContextProvider companyContextProvider;
    private final AuditLogRecorder auditLogRecorder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public SignupRequestService(CompanySignupRequestRepository signupRequestRepository, CompanyService companyService,
            UserService userService, CompanyContextProvider companyContextProvider, AuditLogRecorder auditLogRecorder,
            JavaMailSender mailSender) {
        this.signupRequestRepository = signupRequestRepository;
        this.companyService = companyService;
        this.userService = userService;
        this.companyContextProvider = companyContextProvider;
        this.auditLogRecorder = auditLogRecorder;
        this.mailSender = mailSender;
    }

    public record CreateSignupRequestCommand(String companyName, UUID industryId, String registeredAddress,
            String gstNumber, String adminFullName, String adminEmail, String phoneNumber, String message) {
    }

    @Transactional
    public CompanySignupRequest submit(CreateSignupRequestCommand command) {
        CompanySignupRequest request = new CompanySignupRequest();
        request.setCompanyName(command.companyName());
        request.setIndustryId(command.industryId());
        request.setRegisteredAddress(command.registeredAddress());
        request.setGstNumber(command.gstNumber());
        request.setAdminFullName(command.adminFullName());
        request.setAdminEmail(command.adminEmail());
        request.setPhoneNumber(command.phoneNumber());
        request.setMessage(command.message());
        return signupRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<CompanySignupRequest> listAll() {
        return signupRequestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CompanySignupRequest getById(UUID id) {
        return signupRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CompanySignupRequest", id));
    }

    @Transactional
    public CompanySignupRequest approve(UUID id) {
        CompanySignupRequest request = getById(id);
        requirePending(request);

        Company company = companyService.createCompany(new CompanyService.CreateCompanyCommand(
                request.getCompanyName(), request.getGstNumber(), null, null,
                request.getRegisteredAddress(), request.getIndustryId()));

        String temporaryPassword = generateTemporaryPassword();
        User admin = userService.createCompanyAdmin(company.getId(), request.getAdminEmail(), temporaryPassword,
                request.getAdminFullName());

        request.setStatus(SignupRequestStatus.APPROVED);
        request.setReviewedByUserId(companyContextProvider.getCurrentUserId());
        request.setReviewedAt(Instant.now());
        request.setCreatedCompanyId(company.getId());
        signupRequestRepository.save(request);

        auditLogRecorder.record("SIGNUP_REQUEST_APPROVED", "CompanySignupRequest", request.getId());
        sendApprovalEmail(admin.getEmail(), admin.getFullName(), company.getName(), temporaryPassword);

        return request;
    }

    @Transactional
    public CompanySignupRequest reject(UUID id, String reason) {
        CompanySignupRequest request = getById(id);
        requirePending(request);

        request.setStatus(SignupRequestStatus.REJECTED);
        request.setReviewedByUserId(companyContextProvider.getCurrentUserId());
        request.setReviewedAt(Instant.now());
        request.setRejectionReason(reason);
        signupRequestRepository.save(request);

        auditLogRecorder.record("SIGNUP_REQUEST_REJECTED", "CompanySignupRequest", request.getId());
        return request;
    }

    private void requirePending(CompanySignupRequest request) {
        if (request.getStatus() != SignupRequestStatus.PENDING) {
            throw new InvalidStateException("This request has already been reviewed");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private void sendApprovalEmail(String toEmail, String fullName, String companyName, String temporaryPassword) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(toEmail);
            mail.setSubject("Your AI Compliance Copilot account is ready");
            mail.setText("""
                    Hi %s,

                    Your company "%s" has been approved on AI Compliance Copilot.

                    You can sign in with:
                    Email: %s
                    Temporary password: %s

                    For security, please sign in and reset your password as soon as possible.
                    """.formatted(fullName, companyName, toEmail, temporaryPassword));
            mailSender.send(mail);
        } catch (Exception e) {
            // The company/admin are already created at this point; a failed email must not roll
            // that back. Same trade-off as AuthService's reset-email send - log and move on.
            log.warn("Failed to send signup approval email to {}", toEmail, e);
        }
    }
}
