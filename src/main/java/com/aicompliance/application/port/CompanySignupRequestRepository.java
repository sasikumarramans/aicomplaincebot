package com.aicompliance.application.port;

import com.aicompliance.domain.company.CompanySignupRequest;
import com.aicompliance.domain.company.SignupRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySignupRequestRepository extends JpaRepository<CompanySignupRequest, UUID> {

    List<CompanySignupRequest> findAllByStatus(SignupRequestStatus status);
}
