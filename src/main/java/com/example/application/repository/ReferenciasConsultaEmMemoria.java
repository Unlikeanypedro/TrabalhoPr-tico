package com.example.application.repository;

import com.example.application.model.MedicoConsulta;
import com.example.application.service.ReferenciasConsulta;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Comparator;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ReferenciasConsultaEmMemoria implements ReferenciasConsulta {

    private final ProfissionalRepository profissionais;

    public ReferenciasConsultaEmMemoria() { this.profissionais = null; }

    @org.springframework.beans.factory.annotation.Autowired
    public ReferenciasConsultaEmMemoria(ProfissionalRepository profissionais) {
        this.profissionais = profissionais;
    }

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
        if (profissionais != null) {
            var profissional = profissionais.buscarPorId(medicoId);
            if (profissional.isPresent()) return profissional.map(p -> new MedicoConsulta(p.getId(), p.getEspecialidade()));
        }
        return Optional.ofNullable(medicos.get(medicoId));
    }

    public synchronized List<MedicoConsulta> listarMedicos() {
        Map<Long, MedicoConsulta> disponiveis = new HashMap<>(medicos);
        if (profissionais != null) profissionais.listar().forEach(p ->
            disponiveis.put(p.getId(), new MedicoConsulta(p.getId(), p.getEspecialidade())));
        return disponiveis.values().stream().sorted(Comparator.comparingLong(MedicoConsulta::id)).toList();
    }
}
