package com.example.application.repository;

import java.util.List;
import java.util.Optional;
import com.example.application.model.Consulta;

public interface ConsultaRepository {
    long proximoId();
    List<Consulta> listar();
    Optional<Consulta> buscarPorId(long id);
    void salvar(Consulta consulta);
    void excluir(long id);
}
