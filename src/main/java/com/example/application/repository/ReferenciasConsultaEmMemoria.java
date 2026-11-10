package com.example.application.repository;

import com.example.application.model.MedicoConsulta;
import com.example.application.service.ReferenciasConsulta;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ReferenciasConsultaEmMemoria implements ReferenciasConsulta {

    private final Set<Long> pacientes = new HashSet<>();
    private final Map<Long, MedicoConsulta> medicos = new HashMap<>();

    public synchronized void registrarPaciente(long pacienteId) {
        if (pacienteId <= 0) throw new IllegalArgumentException(
            "ID do paciente deve ser positivo."
        );
        pacientes.add(pacienteId);
    }

    public synchronized void registrarMedico(MedicoConsulta medico) {
        if (medico == null) throw new IllegalArgumentException(
            "Informe o médico responsável."
        );
        medicos.put(medico.id(), medico);
    }

    public synchronized boolean pacienteExiste(long pacienteId) {
        return pacientes.contains(pacienteId);
    }

    public synchronized Optional<MedicoConsulta> buscarMedico(long medicoId) {
        return Optional.ofNullable(medicos.get(medicoId));
    }
}
