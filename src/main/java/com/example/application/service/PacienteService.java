package com.example.application.service;

import com.example.application.model.Paciente;
import com.example.application.repository.PacienteRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

    private final PacienteRepository repository;

    @Autowired
    public PacienteService(PacienteRepository repository) {
        this.repository = repository;
    }

    public List<Paciente> listar() {
        return repository.listar();
    }

    public Paciente buscarPorId(long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado: " + id));
    }

    public Paciente cadastrar(Paciente paciente) {
        return repository.salvar(paciente);
    }

    public void excluir(long id) {
        repository.excluir(id);
    }
}