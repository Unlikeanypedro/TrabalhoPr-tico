package com.example.application.repository;

import com.example.application.model.Paciente;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository {
    List<Paciente> listar();
    Optional<Paciente> buscarPorId(long id);
    Paciente salvar(Paciente paciente);
    void excluir(long id);
}