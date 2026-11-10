package com.example.application.service;

import com.example.application.model.MedicoConsulta;
import java.util.Optional;

public interface ReferenciasConsulta {
    boolean pacienteExiste(long pacienteId);
    Optional<MedicoConsulta> buscarMedico(long medicoId);
}
