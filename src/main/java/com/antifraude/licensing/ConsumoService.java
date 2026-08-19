package com.antifraude.licensing;

import com.antifraude.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Service
public class ConsumoService {

    private final UsoSuscripcionRepository usoSuscripcionRepository;
    private final EmpresaRepository empresaRepository;

    public ConsumoService(UsoSuscripcionRepository usoSuscripcionRepository,
                          EmpresaRepository empresaRepository) {
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public UsoSuscripcion usoActual(UUID empresaId) {
        YearMonth periodo = YearMonth.now();
        return usoSuscripcionRepository.findFirstByEmpresaIdAndAnioAndMesOrderByIdDesc(empresaId, periodo.getYear(), periodo.getMonthValue())
                .orElseGet(() -> nuevaUso(empresaId, periodo.getYear(), periodo.getMonthValue()));
    }

    @Transactional(readOnly = true)
    public int transaccionesDelMes(UUID empresaId) {
        return usoActual(empresaId).getTransaccionesProcesadas();
    }

    @Transactional(readOnly = true)
    public int consultasKycDelMes(UUID empresaId) {
        return usoActual(empresaId).getConsultasKyc();
    }

    @Transactional(readOnly = true)
    public int reportesDelMes(UUID empresaId) {
        return usoActual(empresaId).getReportesGenerados();
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

    private UsoSuscripcion nuevaUso(UUID empresaId, int anio, int mes) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
        return UsoSuscripcion.builder()
                .empresa(empresa)
                .anio(anio)
                .mes(mes)
                .usuariosActivos(0)
                .transaccionesProcesadas(0)
                .consultasKyc(0)
                .alertasGeneradas(0)
                .reportesGenerados(0)
                .build();
    }
}
