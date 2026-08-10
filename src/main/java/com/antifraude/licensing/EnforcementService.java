package com.antifraude.licensing;

import com.antifraude.exception.BusinessException;
import com.antifraude.exception.QuotaExceededException;
import com.antifraude.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EnforcementService {

    private final SuscripcionRepository suscripcionRepository;
    private final EmpresaRepository empresaRepository;
    private final ConsumoService consumoService;
    private final ObjectMapper objectMapper;

    public EnforcementService(SuscripcionRepository suscripcionRepository,
                              EmpresaRepository empresaRepository,
                              ConsumoService consumoService,
                              ObjectMapper objectMapper) {
        this.suscripcionRepository = suscripcionRepository;
        this.empresaRepository = empresaRepository;
        this.consumoService = consumoService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public void verificarSuscripcionVigente(UUID empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
        if (empresa.getEstado() != Empresa.EstadoEmpresa.ACTIVA) {
            throw new BusinessException("EMPRESA_INACTIVA",
                    "La empresa no se encuentra activa. Contacte a su administrador.");
        }
        suscripcionVigente(empresaId);
    }

    @Transactional(readOnly = true)
    public void verificarModulo(UUID empresaId, String modulo) {
        if (!moduloHabilitado(empresaId, modulo)) {
            throw new BusinessException("MODULO_NO_INCLUIDO",
                    "El modulo " + modulo + " no esta incluido en el plan contratado.");
        }
    }

    @Transactional(readOnly = true)
    public boolean moduloHabilitado(UUID empresaId, String modulo) {
        PlanLicencia plan = planVigente(empresaId);
        return modulosDe(plan).stream().anyMatch(mod -> mod.equalsIgnoreCase(modulo));
    }

    @Transactional(readOnly = true)
    public void verificarLimiteTransacciones(UUID empresaId) {
        verificarLimite(planVigente(empresaId).getLimiteTransaccionesMensuales(),
                consumoService.transaccionesDelMes(empresaId),
                "LIMITE_TRANSACCIONES_MENSUAL",
                "Transacciones");
    }

    @Transactional(readOnly = true)
    public void verificarLimiteKyc(UUID empresaId) {
        verificarLimite(planVigente(empresaId).getLimiteConsultasKycMensuales(),
                consumoService.consultasKycDelMes(empresaId),
                "LIMITE_CONSULTAS_KYC_MENSUAL",
                "Consultas KYC");
    }

    @Transactional(readOnly = true)
    public void verificarLimiteReportes(UUID empresaId) {
        verificarLimite(planVigente(empresaId).getLimiteReportesMensuales(),
                consumoService.reportesDelMes(empresaId),
                "LIMITE_REPORTES_MENSUAL",
                "Reportes");
    }

    @Transactional(readOnly = true)
    public Integer limiteReglas(UUID empresaId) {
        return planVigente(empresaId).getLimiteReglas();
    }

    @Transactional(readOnly = true)
    public Integer limiteHistorial(UUID empresaId) {
        return planVigente(empresaId).getLimiteHistorialTransaccional();
    }

    @Transactional(readOnly = true)
    public Integer limitePerfilesRiesgo(UUID empresaId) {
        return planVigente(empresaId).getLimiteEscenarios();
    }

    @Transactional(readOnly = true)
    public void verificarLimiteReglas(UUID empresaId, long usadas) {
        Integer limite = limiteReglas(empresaId);
        if (limite != null && usadas >= limite) {
            throw new QuotaExceededException("LIMITE_REGLAS_PLAN",
                    "Se alcanzo el limite de reglas del plan contratado (" + limite + "). "
                            + "Elimine reglas o contrate un plan superior.");
        }
    }

    @Transactional(readOnly = true)
    public PlanLicencia planVigente(UUID empresaId) {
        return suscripcionVigente(empresaId).getPlanLicencia();
    }

    private Suscripcion suscripcionVigente(UUID empresaId) {
        LocalDate hoy = LocalDate.now();
        return suscripcionRepository.findByEmpresaId(empresaId).stream()
                .filter(suscripcion -> suscripcion.getFechaFin() != null && !suscripcion.getFechaFin().isBefore(hoy))
                .filter(suscripcion -> suscripcion.getEstado() == Suscripcion.EstadoSuscripcion.ACTIVA
                        || suscripcion.getEstado() == Suscripcion.EstadoSuscripcion.POR_VENCER)
                .findFirst()
                .orElseThrow(() -> new BusinessException("SUSCRIPCION_INACTIVA",
                        "La empresa no tiene una suscripcion vigente. Verifique su plan y fecha de vencimiento."));
    }

    private List<String> modulosDe(PlanLicencia plan) {
        if (plan == null || plan.getModulosIncluidosJson() == null || plan.getModulosIncluidosJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(plan.getModulosIncluidosJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException("PLAN_MODULOS_INVALIDOS",
                    "El plan " + plan.getCodigo() + " tiene una configuracion de modulos invalida.");
        }
    }

    private void verificarLimite(Integer limite, int usado, String code, String concepto) {
        if (limite != null && usado >= limite) {
            throw new QuotaExceededException(code,
                    "Se alcanzo el limite mensual de " + concepto + " para el plan contratado (" + limite + ").");
        }
    }
}