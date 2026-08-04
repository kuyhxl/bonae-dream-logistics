package com.bonae.logistics.common.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    public static final String SYSTEM = "SYSTEM";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(attributes == null) return Optional.of(SYSTEM);

        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader(USER_ID_HEADER);

        return Optional.of(
                userId == null || userId.isBlank() ? SYSTEM : userId
        );
    }
}
