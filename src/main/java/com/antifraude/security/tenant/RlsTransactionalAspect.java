package com.antifraude.security.tenant;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RlsTransactionalAspect {

    private final RlsContextService rlsContextService;

    public RlsTransactionalAspect(RlsContextService rlsContextService) {
        this.rlsContextService = rlsContextService;
    }

    @Before("(@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(org.springframework.transaction.annotation.Transactional)) && " +
            "!within(com.antifraude.security.tenant.RlsContextService)")
    public void applyRlsContext() {
        rlsContextService.applyCurrentContext();
    }
}
