package com.antifraude.assignment;

import com.antifraude.alerts.Alerta;
import com.antifraude.alerts.AlertaRepository;
import com.antifraude.alerts.EstadisticaCargaAnalista;
import com.antifraude.alerts.EstadisticaCargaAnalistaRepository;
import com.antifraude.dto.WorkloadResponse;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkloadService {

    private static final Logger log = LoggerFactory.getLogger(WorkloadService.class);

    private final UsuarioRepository usuarioRepository;
    private final AlertaRepository alertaRepository;
    private final EstadisticaCargaAnalistaRepository cargaRepository;

    public WorkloadService(UsuarioRepository usuarioRepository,
                            AlertaRepository alertaRepository,
                            EstadisticaCargaAnalistaRepository cargaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alertaRepository = alertaRepository;
        this.cargaRepository = cargaRepository;
    }

    public List<WorkloadResponse> obtenerCargaTodos() {
        List<Usuario> analistas = usuarioRepository.findAll().stream()
                .filter(u -> "ANALISTA".equals(u.getRol()) && u.getActivo())
                .toList();

        return analistas.stream().map(analista -> {
            int pendientes = (int) alertaRepository.countByAsignadoAIdAndEstadoIn(
                    analista.getId(), List.of("PENDIENTE", "ASIGNADA", "INVESTIGANDO"));

            EstadisticaCargaAnalista stats = cargaRepository
                    .findByUsuarioIdAndFecha(analista.getId(), LocalDate.now())
                    .orElse(null);

            return new WorkloadResponse(
                    analista.getId(),
                    analista.getNombre(),
                    stats != null ? stats.getAlertasAsignadas() : 0,
                    pendientes,
                    stats != null ? stats.getTiempoPromedioResolucion() : 0L);
        }).toList();
    }
}
