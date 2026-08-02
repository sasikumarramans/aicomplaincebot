package com.aicompliance.presentation.dto.response;

import com.aicompliance.domain.audit.AuditFolder;
import java.time.Instant;
import java.util.UUID;

public record AuditFolderResponse(
        UUID id,
        String status,
        Instant generatedAt,
        Instant expiresAt,
        String failureReason) {

    public static AuditFolderResponse from(AuditFolder folder) {
        return new AuditFolderResponse(folder.getId(), folder.getStatus().name(), folder.getGeneratedAt(),
                folder.getExpiresAt(), folder.getFailureReason());
    }
}
