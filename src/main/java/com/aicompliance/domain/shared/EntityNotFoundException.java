package com.aicompliance.domain.shared;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, Object identifier) {
        super("%s not found: %s".formatted(entityName, identifier));
    }
}
