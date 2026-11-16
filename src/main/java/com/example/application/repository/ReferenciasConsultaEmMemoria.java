package com.example.application.repository;

import com.example.application.model.MedicoConsulta;
import com.example.application.model.Paciente;
import com.example.application.service.ReferenciasConsulta;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ReferenciasConsultaEmMemoria implements ReferenciasConsulta {

    private final ProfissionalRepository profissionais;
    private final PacienteRepository pacienteRepository;

    private final Set<Long> pacientes = new HashSet<>();
    private final Set<MedicoConsulta> medicos = new HashSet<>();

    public ReferenciasConsultaEmMemoria() {
        this.profissionais = null;
        this.pacienteRepository = null;
    }

    public ReferenciasConsultaEmMemoria(ProfissionalRepository profissionais) {
        this.profissionais = profissionais;
        this.pacienteRepository = null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ReferenciasConsultaEmMemoria(ProfissionalRepository profissionais, PacienteRepository pacienteRepository) {
        this.profissionais = profissionais;
        this.pacienteRepository = pacienteRepository;
    }

    public void registrarPaciente(long pacienteId) {
        if (pacienteId <= 0) throw new IllegalArgumentException(
            "ID do paciente deve ser positivo."
        );
        if (pacienteRepository != null) {
            var paciente = new Paciente();
            paciente.setId(pacienteId);
            pacienteRepository.salvar(paciente);
        } else {
            pacientes.add(pacienteId);
        }
    }

    public void registrarMedico(MedicoConsulta medico) {
        if (medico == null) throw new IllegalArgumentException(
            "Informe o médico responsável."
        );
        medicos.add(medico);
    }

    @Override
    public boolean pacienteExiste(long pacienteId) {
        if (pacienteRepository != null) {
            return pacienteRepository.buscarPorId(pacienteId).isPresent();
        }
        return pacientes.contains(pacienteId);
    }

    @Override
    public Optional<MedicoConsulta> buscarMedico(long medicoId) {
        if (profissionais != null) {
            var profissional = profissionais.buscarPorId(medicoId);
            if (profissional.isPresent()) return profissional.map(p -> new MedicoConsulta(p.getId(), p.getEspecialidade()));
        }
        for (var m : medicos) {
            if (m.id() == medicoId) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public List<MedicoConsulta> listarMedicos() {
        Map<Long, MedicoConsulta> disponiveis = new HashMap<>();
        if (profissionais != null) profissionais.listar().forEach(p ->
            disponiveis.put(p.getId(), new MedicoConsulta(p.getId(), p.getEspecialidade())));
        disponiveis.putAll(
            medicos.stream().collect(java.util.stream.Collectors.toMap(MedicoConsulta::id, m -> m))
        );
        return disponiveis.values().stream().sorted(Comparator.comparingLong(MedicoConsulta::id)).toList();
    }
}