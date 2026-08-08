package com.antifraude.security.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mantiene una unica transaccion durante cada peticion autenticada. Esto es
 * necesario porque PostgreSQL recibe el tenant mediante SET LOCAL y, por
 * diseno, descarta ese valor al terminar la transaccion.
 */
@Component
public class TenantTransactionFilter extends OncePerRequestFilter {

    private final TransactionTemplate transactionTemplate;
    private final RlsContextService rlsContextService;

    public TenantTransactionFilter(PlatformTransactionManager transactionManager,
                                   RlsContextService rlsContextService) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.rlsContextService = rlsContextService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (TenantContext.getEmpresaId() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> {
                rlsContextService.applyCurrentContext();
                try {
                    filterChain.doFilter(request, response);
                } catch (IOException | ServletException exception) {
                    status.setRollbackOnly();
                    throw new RequestTransactionException(exception);
                }
            });
        } catch (RequestTransactionException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw (ServletException) exception.getCause();
        }
    }

    private static final class RequestTransactionException extends RuntimeException {
        private RequestTransactionException(Exception cause) {
            super(cause);
        }
    }
}
