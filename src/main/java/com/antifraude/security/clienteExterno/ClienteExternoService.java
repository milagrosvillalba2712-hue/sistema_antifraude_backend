package com.antifraude.security.clienteExterno;

import com.antifraude.common.entity.ClienteExterno;
import com.antifraude.common.entity.ClienteExternoAuditoria;
import com.antifraude.common.repository.ClienteExternoAuditoriaRepository;
import com.antifraude.common.repository.ClienteExternoRepository;
import com.antifraude.exception.BusinessException;
import com.antifraude.users.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClienteExternoService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ClienteExternoRepository repository;
    private final ClienteExternoAuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteExternoService(ClienteExternoRepository repository,
                                ClienteExternoAuditoriaRepository auditoriaRepository,
                                UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record RotarApiKeyResult(String apiKey, String prefix, String last4) {}

    @Transactional
    public Optional<ClienteExterno> validarApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }

        for (ClienteExterno cliente : repository.findByActivoTrueOrderByNombreAsc()) {
            if (passwordEncoder.matches(rawKey, cliente.getApiKeyHash())) {
                if (cliente.getFechaExpiracion() != null
                        && cliente.getFechaExpiracion().isBefore(OffsetDateTime.now())) {
                    return Optional.empty();
                }
                repository.actualizarFechaUltimoUso(cliente.getId(), OffsetDateTime.now());
                return Optional.of(cliente);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public RotarApiKeyResult rotarApiKey(UUID id, UUID usuarioModificacionId) {
        ClienteExterno cliente = repository.findById(id)
                .orElseThrow(() -> new BusinessException("CLIENTE_EXTERNO_NO_ENCONTRADO",
                        "Cliente externo no encontrado"));

        String rawKey = generarRawApiKey();
        String prefix = cliente.getApiKeyPrefix();
        String fullKey = prefix + rawKey;
        String last4 = fullKey.substring(fullKey.length() - 4);
        cliente.setApiKeyHash(passwordEncoder.encode(fullKey));
        cliente.setApiKeyLast4(last4);
        if (usuarioModificacionId != null) {
            usuarioRepository.findById(usuarioModificacionId).ifPresent(cliente::setUsuarioModificacion);
        }
        repository.save(cliente);

        return new RotarApiKeyResult(fullKey, prefix, last4);
    }

    @Transactional(readOnly = true)
    public Page<ClienteExternoAuditoria> auditoria(UUID clienteExternoId, int page, int size) {
        return auditoriaRepository.findByClienteExternoIdOrderByFechaHoraCreacionDesc(
                clienteExternoId, PageRequest.of(page, size));
    }

    @Async
    public void registrarAuditoriaAsync(UUID clienteExternoId, String endpoint, String metodo,
                                       String ipOrigen, int status, long duracionMs,
                                       String errorCode, String requestId) {
        try {
            ClienteExternoAuditoria audit = ClienteExternoAuditoria.builder()
                    .clienteExternoId(clienteExternoId)
                    .endpoint(endpoint)
                    .metodoHttp(metodo)
                    .ipOrigen(ipOrigen)
                    .status(status)
                    .duracionMs((int) duracionMs)
                    .errorCode(errorCode)
                    .requestId(requestId)
                    .build();
            auditoriaRepository.save(audit);
        } catch (Exception ignored) {
        }
    }

    private String generarRawApiKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
