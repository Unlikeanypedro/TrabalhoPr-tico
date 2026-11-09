package com.example.application.service;

import java.util.Optional;
import com.example.application.model.MedicoConsulta;

public interface ReferenciasConsulta {
    boolean pacienteExiste(long pacienteId);
    Optional<MedicoConsulta> buscarMedico(long medicoId);
}
