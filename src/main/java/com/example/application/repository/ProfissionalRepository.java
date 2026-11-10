package com.example.application.repository;

import com.example.application.model.Profissional;
import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository {
    long proximoId();
    void salvar(Profissional profissional);
    Optional<Profissional> buscarPorId(long id);
    List<Profissional> listar();
    void excluir(long id);
}
