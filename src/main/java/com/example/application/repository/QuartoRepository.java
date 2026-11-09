package com.example.application.repository;

import java.util.List;
import java.util.Optional;
import com.example.application.model.Quarto;

public interface QuartoRepository {
    long proximoId();
    List<Quarto> listar();
    Optional<Quarto> buscarPorId(long id);
    void salvar(Quarto quarto);
    void excluir(long id);
}
