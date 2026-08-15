package com.antifraude.escenarios;

import com.antifraude.common.entity.Escenario;
import com.antifraude.common.repository.EscenarioRepository;
import com.antifraude.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EscenarioService {

    private final EscenarioRepository escenarioRepository;

    public EscenarioService(EscenarioRepository escenarioRepository) {
        this.escenarioRepository = escenarioRepository;
    }

    public List<Escenario> listarTodos() {
        return escenarioRepository.findAll();
    }

    public Escenario buscarPorId(Long id) {
        return escenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escenario", "id", id));
    }

    public Escenario crear(Escenario escenario) {
        return escenarioRepository.save(escenario);
    }

    public Escenario actualizar(Long id, Escenario actualizado) {
        Escenario escenario = buscarPorId(id);
        escenario.setCodigo(actualizado.getCodigo());
        escenario.setNombre(actualizado.getNombre());
        escenario.setDescripcion(actualizado.getDescripcion());
        return escenarioRepository.save(escenario);
    }

    public void eliminar(Long id) {
        Escenario escenario = buscarPorId(id);
        escenario.setActivo(false);
        escenarioRepository.save(escenario);
    }
}
