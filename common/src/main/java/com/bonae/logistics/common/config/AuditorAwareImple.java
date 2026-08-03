package com.bonae.logistics.common.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class AuditorAwareImple implements AuditorAware<String> {

    public static final String SYSTEM = "SYSTEM";
    private static final String USER_ID_HEADR = "X-User_Id";

    @Override
    public Optional<String> getCurrentAuditor() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(attributes == null) return Optional.of(SYSTEM);

        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader(USER_ID_HEADR);

        return Optional.of(
                userId == null || userId.isBlank() ? SYSTEM : userId
        );
    }
}
