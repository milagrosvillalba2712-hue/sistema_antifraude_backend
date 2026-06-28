package com.antifraude.assignment;

import com.antifraude.alerts.Alerta;
import com.antifraude.users.Usuario;

import java.util.List;

public interface AssignmentStrategy {
    Usuario asignar(Alerta alerta, List<Usuario> candidatos);
}
