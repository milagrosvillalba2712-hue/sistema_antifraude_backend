package com.antifraude.admin.clienteExterno;

import com.antifraude.common.entity.ClienteExterno;
import com.antifraude.common.entity.ClienteExternoAuditoria;
import com.antifraude.common.repository.ClienteExternoRepository;
import com.antifraude.dto.ClienteExternoConKeyResponse;
import com.antifraude.dto.ClienteExternoResponse;
import com.antifraude.exception.BusinessException;
import com.antifraude.security.clienteExterno.ClienteExternoService;
import com.antifraude.security.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Autogestion de la API key propia del banco para consumir su motor antifraude
 * (POST /api/transacciones). No se crean ni gestionan clientes externos de
 * terceros; solo la key privada de la empresa actual.
 */
@RestController
@RequestMapping("/api/admin/clientes-externos")
@Transactional(readOnly = true)
public class AdminClienteExternoController {

    private final ClienteExternoRepository repository;
    private final ClienteExternoService clienteExternoService;

    public AdminClienteExternoController(ClienteExternoRepository repository,
                                        ClienteExternoService clienteExternoService) {
        this.repository = repository;
        this.clienteExternoService = clienteExternoService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteExternoResponse>> listar() {
        return ResponseEntity.ok(repository.findByEmpresaIdOrderByNombreAsc(empresaActual())
                .stream()
                .map(ClienteExternoResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteExternoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ClienteExternoResponse.from(obtenerPropio(id)));
    }

    @PostMapping("/{id}/rotar")
    @Transactional
    public ResponseEntity<ClienteExternoConKeyResponse> rotarKey(@PathVariable UUID id) {
        ClienteExterno cliente = obtenerPropio(id);
        ClienteExternoService.RotarApiKeyResult result =
                clienteExternoService.rotarApiKey(cliente.getId(), TenantContext.getUsuarioId());
        return ResponseEntity.ok(ClienteExternoConKeyResponse.crear(result.apiKey(), cliente));
    }

    @GetMapping("/{id}/auditoria")
    public ResponseEntity<Page<ClienteExternoAuditoria>> auditoria(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        obtenerPropio(id);
        return ResponseEntity.ok(clienteExternoService.auditoria(id, page, size));
    }

    private UUID empresaActual() {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("EMPRESA_NO_DETERMINADA",
                    "No se pudo determinar la empresa del usuario actual");
        }
        return empresaId;
    }

    private ClienteExterno obtenerPropio(UUID id) {
        ClienteExterno cliente = repository.findById(id)
                .orElseThrow(() -> new BusinessException("CLIENTE_EXTERNO_NO_ENCONTRADO",
                        "Cliente externo no encontrado"));
        UUID empresaId = empresaActual();
        if (cliente.getEmpresa() == null || !empresaId.equals(cliente.getEmpresa().getId())) {
            throw new BusinessException("CLIENTE_EXTERNO_NO_AUTORIZADO",
                    "El cliente externo no pertenece a la empresa actual");
        }
        return cliente;
    }
}
