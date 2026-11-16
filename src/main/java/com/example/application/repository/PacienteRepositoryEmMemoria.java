package com.example.application.repository;

import com.example.application.model.Paciente;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PacienteRepositoryEmMemoria implements PacienteRepository {

    private final Map<Long, Paciente> pacientes = new HashMap<>();
    private long proximoId = 1;

    @Override
    public List<Paciente> listar() {
        return new ArrayList<>(pacientes.values());
    }

    @Override
    public Optional<Paciente> buscarPorId(long id) {
        return Optional.ofNullable(pacientes.get(id));
    }

    @Override
    public Paciente salvar(Paciente paciente) {
        if (paciente.getId() <= 0) {
            paciente.setId(proximoId++);
        }
        pacientes.put(paciente.getId(), paciente);
        return paciente;
    }

    @Override
    public void excluir(long id) {
        pacientes.remove(id);
    }
}