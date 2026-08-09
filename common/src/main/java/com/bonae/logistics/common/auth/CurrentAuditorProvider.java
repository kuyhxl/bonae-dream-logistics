package com.bonae.logistics.common.auth;

import com.bonae.logistics.common.config.AuditorAwareImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentAuditorProvider {

    private final AuditorAware<String> auditorAware;

    public String getCurrentAuditorOrSystem() {
        return auditorAware.getCurrentAuditor().orElse(AuditorAwareImpl.SYSTEM);
    }
}
