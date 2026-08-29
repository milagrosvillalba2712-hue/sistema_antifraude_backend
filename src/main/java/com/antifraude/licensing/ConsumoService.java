package com.antifraude.licensing;

import com.antifraude.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConsumoService {

    private final UsoSuscripcionRepository usoSuscripcionRepository;
    private final EmpresaRepository empresaRepository;
    private final SuscripcionRepository suscripcionRepository;

    public ConsumoService(UsoSuscripcionRepository usoSuscripcionRepository,
                          EmpresaRepository empresaRepository,
                          SuscripcionRepository suscripcionRepository) {
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.empresaRepository = empresaRepository;
        this.suscripcionRepository = suscripcionRepository;
    }

    @Transactional
    public UsoSuscripcion usoActual(UUID empresaId) {
        YearMonth periodo = YearMonth.now();
        Suscripcion suscripcion = suscripcionVigente(empresaId);
        return buscarUsoActual(empresaId, suscripcion, periodo)
                .orElseGet(() -> nuevaUso(empresaId, suscripcion, periodo));
    }

    @Transactional(readOnly = true)
    public int transaccionesDelMes(UUID empresaId) {
        YearMonth periodo = YearMonth.now();
        Suscripcion suscripcion = suscripcionVigente(empresaId);
        return buscarUsoActual(empresaId, suscripcion, periodo)
                .map(UsoSuscripcion::getTransaccionesProcesadas)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int consultasKycDelMes(UUID empresaId) {
        YearMonth periodo = YearMonth.now();
        Suscripcion suscripcion = suscripcionVigente(empresaId);
        return buscarUsoActual(empresaId, suscripcion, periodo)
                .map(UsoSuscripcion::getConsultasKyc)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int reportesDelMes(UUID empresaId) {
        YearMonth periodo = YearMonth.now();
        Suscripcion suscripcion = suscripcionVigente(empresaId);
        return buscarUsoActual(empresaId, suscripcion, periodo)
                .map(UsoSuscripcion::getReportesGenerados)
                .orElse(0);
    }

    @Transactional
    public void registrarTransaccion(UUID empresaId) {
        UsoSuscripcion uso = usoActual(empresaId);
        uso.setTransaccionesProcesadas(uso.getTransaccionesProcesadas() + 1);
        usoSuscripcionRepository.save(uso);
    }

    @Transactional
    public void registrarConsultaKyc(UUID empresaId) {
        UsoSuscripcion uso = usoActual(empresaId);
        uso.setConsultasKyc(uso.getConsultasKyc() + 1);
        usoSuscripcionRepository.save(uso);
    }

    @Transactional
    public void registrarReporte(UUID empresaId) {
        UsoSuscripcion uso = usoActual(empresaId);
        uso.setReportesGenerados(uso.getReportesGenerados() + 1);
        usoSuscripcionRepository.save(uso);
    }

    private Optional<UsoSuscripcion> buscarUsoActual(UUID empresaId, Suscripcion suscripcion, YearMonth periodo) {
        return usoSuscripcionRepository.findFirstByEmpresaIdAndSuscripcionIdAndPeriodoOrderByIdDesc(
                empresaId,
                suscripcion.getId(),
                periodo.atDay(1)
        );
    }

    private UsoSuscripcion nuevaUso(UUID empresaId, Suscripcion suscripcion, YearMonth periodo) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
        return UsoSuscripcion.builder()
                .empresa(empresa)
                .suscripcion(suscripcion)
                .periodo(periodo.atDay(1))
                .anio(periodo.getYear())
                .mes(periodo.getMonthValue())
                .usuariosActivos(0)
                .transaccionesProcesadas(0)
                .consultasKyc(0)
                .alertasGeneradas(0)
                .reportesGenerados(0)
                .build();
    }

    private Suscripcion suscripcionVigente(UUID empresaId) {
        LocalDate hoy = LocalDate.now();
        return suscripcionRepository.findByEmpresaId(empresaId).stream()
                .filter(suscripcion -> suscripcion.getFechaFin() != null && !suscripcion.getFechaFin().isBefore(hoy))
                .filter(suscripcion -> suscripcion.getEstado() == Suscripcion.EstadoSuscripcion.ACTIVA
                        || suscripcion.getEstado() == Suscripcion.EstadoSuscripcion.POR_VENCER)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", "empresaId", empresaId));
    }
}
