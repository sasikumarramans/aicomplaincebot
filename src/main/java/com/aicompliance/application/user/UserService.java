package com.aicompliance.application.user;

import com.aicompliance.application.audit.AuditLogRecorder;
import com.aicompliance.application.port.CompanyContextProvider;
import com.aicompliance.application.port.UserRepository;
import com.aicompliance.domain.shared.EntityNotFoundException;
import com.aicompliance.domain.shared.InvalidStateException;
import com.aicompliance.domain.user.Role;
import com.aicompliance.domain.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CompanyContextProvider companyContextProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRecorder auditLogRecorder;

    public UserService(UserRepository userRepository, CompanyContextProvider companyContextProvider,
            PasswordEncoder passwordEncoder, AuditLogRecorder auditLogRecorder) {
        this.userRepository = userRepository;
        this.companyContextProvider = companyContextProvider;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRecorder = auditLogRecorder;
    }

    public record CreateUserCommand(String email, String rawPassword, String fullName, Role role, UUID plantId,
            String department, boolean temporary, Instant accessExpiresAt) {
    }

    /**
     * Creates the first COMPANY_ADMIN for a newly created company. Only callable in a
     * SUPER_ADMIN context (no tenant filter active), so companyId must be supplied explicitly.
     */
    @Transactional
    public User createCompanyAdmin(UUID companyId, String email, String rawPassword, String fullName) {
        requireUniqueEmail(email);
        User user = new User();
        user.setCompanyId(companyId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(Role.COMPANY_ADMIN);
        user = userRepository.save(user);
        auditLogRecorder.record("USER_CREATED", "User", user.getId());
        return user;
    }

    @Transactional
    public User createUser(CreateUserCommand command) {
        if (command.role() == Role.SUPER_ADMIN || command.role() == Role.COMPANY_ADMIN) {
            throw new InvalidStateException("Use dedicated endpoints to create SUPER_ADMIN or COMPANY_ADMIN users");
        }
        requireUniqueEmail(command.email());

        User user = new User();
        user.setCompanyId(companyContextProvider.getCurrentCompanyId());
        user.setEmail(command.email());
        user.setPasswordHash(passwordEncoder.encode(command.rawPassword()));
        user.setFullName(command.fullName());
        user.setRole(command.role());
        user.setPlantId(command.plantId());
        user.setDepartment(command.department());

        if (command.role() == Role.AUDITOR) {
            user.setTemporary(command.temporary());
            user.setAccessExpiresAt(command.accessExpiresAt());
        }

        user = userRepository.save(user);
        auditLogRecorder.record("USER_CREATED", "User", user.getId());
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> listForCurrentCompany() {
        return userRepository.findAllByCompanyId(companyContextProvider.getCurrentCompanyId());
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    @Transactional
    public User setActive(UUID id, boolean active) {
        User user = getById(id);
        user.setActive(active);
        return userRepository.save(user);
    }

    private void requireUniqueEmail(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new InvalidStateException("Email already in use: " + email);
        }
    }
}
