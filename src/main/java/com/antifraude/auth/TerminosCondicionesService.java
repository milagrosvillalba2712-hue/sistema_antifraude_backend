package com.antifraude.auth;

import com.antifraude.common.entity.DocumentoLegal;
import com.antifraude.common.entity.TipoDocumentoLegal;
import com.antifraude.common.entity.UsuarioDocumentoLegal;
import com.antifraude.common.repository.DocumentoLegalRepository;
import com.antifraude.common.repository.UsuarioDocumentoLegalRepository;
import com.antifraude.dto.DocumentoLegalResponseDTO;
import com.antifraude.dto.PendienteAceptacionDTO;
import com.antifraude.exception.BusinessException;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import com.antifraude.licensing.Empresa;
import com.antifraude.licensing.EmpresaRepository;
import com.antifraude.security.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TerminosCondicionesService {

    private static final Logger log = LoggerFactory.getLogger(TerminosCondicionesService.class);

    private final DocumentoLegalRepository documentoLegalRepository;
    private final UsuarioDocumentoLegalRepository usuarioDocumentoLegalRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public TerminosCondicionesService(DocumentoLegalRepository documentoLegalRepository,
                                       UsuarioDocumentoLegalRepository usuarioDocumentoLegalRepository,
                                       UsuarioRepository usuarioRepository,
                                       EmpresaRepository empresaRepository) {
        this.documentoLegalRepository = documentoLegalRepository;
        this.usuarioDocumentoLegalRepository = usuarioDocumentoLegalRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    /**
     * Obtiene los documentos legales pendientes de aceptación para un usuario.
     */
    @Transactional(readOnly = true)
    public PendienteAceptacionDTO obtenerPendientes(UUID usuarioId) {
        UUID empresaId = TenantContext.getEmpresaId();
        List<DocumentoLegal> activos = documentoLegalRepository.findByActivoTrue();

        List<DocumentoLegalResponseDTO> pendientes = new ArrayList<>();
        for (DocumentoLegal doc : activos) {
            boolean aceptado = usuarioDocumentoLegalRepository
                    .existsByUsuarioIdAndDocumentoLegalIdAndAceptoTrue(usuarioId, doc.getId());
            if (!aceptado) {
                pendientes.add(toResponse(doc));
            }
        }

        return new PendienteAceptacionDTO(!pendientes.isEmpty(), pendientes);
    }

    /**
     * Registra la aceptación o rechazo de un documento legal por un usuario.
     */
    @Transactional
    public void aceptarDocumento(UUID usuarioId, Long documentoLegalId, boolean acepto,
                                  String ipAddress, String userAgent) {
        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            throw new BusinessException("TENANT_NO_DISPONIBLE", "No se pudo determinar la empresa");
        }

        DocumentoLegal documento = documentoLegalRepository.findById(documentoLegalId)
                .orElseThrow(() -> new BusinessException("DOCUMENTO_NO_ENCONTRADO", "Documento legal no encontrado"));

        if (!documento.getActivo()) {
            throw new BusinessException("DOCUMENTO_INACTIVO", "El documento legal no esta activo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("EMPRESA_NO_ENCONTRADA", "Empresa no encontrada"));

        UsuarioDocumentoLegal registro = usuarioDocumentoLegalRepository
                .findByUsuarioIdAndDocumentoLegalId(usuarioId, documentoLegalId)
                .orElse(null);

        if (registro == null) {
            registro = UsuarioDocumentoLegal.builder()
                    .usuario(usuario)
                    .documentoLegal(documento)
                    .acepto(acepto)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            registro.setEmpresaId(empresaId);
            if (acepto) {
                registro.setFechaAceptacion(OffsetDateTime.now());
            }
        } else {
            registro.setAcepto(acepto);
            if (acepto) {
                registro.setFechaAceptacion(OffsetDateTime.now());
            } else {
                registro.setFechaAceptacion(null);
            }
            registro.setIpAddress(ipAddress);
            registro.setUserAgent(userAgent);
        }

        usuarioDocumentoLegalRepository.save(registro);
        log.info("[TC] Usuario {} {} documento legal {} (v{}) - empresa {}",
                usuarioId, acepto ? "acepto" : "rechazo",
                documento.getTipo(), documento.getVersion(), empresaId);
    }

    /**
     * Verifica si un usuario tiene documentos pendientes de aceptación.
     */
    @Transactional(readOnly = true)
    public boolean tienePendientes(UUID usuarioId) {
        return obtenerPendientes(usuarioId).requiereAceptacion();
    }

    /**
     * Obtiene la versión activa más reciente de un tipo de documento.
     */
    @Transactional(readOnly = true)
    public DocumentoLegal obtenerVersionActiva(TipoDocumentoLegal tipo) {
        return documentoLegalRepository.findFirstByTipoAndActivoTrueOrderByVersionDesc(tipo)
                .orElseThrow(() -> new BusinessException("DOCUMENTO_NO_ENCONTRADO",
                        "No hay version activa para el tipo: " + tipo));
    }

    /**
     * Lista todos los documentos legales activos.
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegalResponseDTO> listarActivos() {
        return documentoLegalRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un documento legal por ID.
     */
    @Transactional(readOnly = true)
    public DocumentoLegalResponseDTO obtenerPorId(Long id) {
        DocumentoLegal doc = documentoLegalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DOCUMENTO_NO_ENCONTRADO", "Documento legal no encontrado"));
        return toResponse(doc);
    }

    private DocumentoLegalResponseDTO toResponse(DocumentoLegal doc) {
        return new DocumentoLegalResponseDTO(
                doc.getId(),
                doc.getTipo(),
                doc.getVersion(),
                doc.getTitulo(),
                doc.getContenido(),
                doc.getUrlDocumento(),
                doc.getFechaPublicacion()
        );
    }
}
