package com.example.application.repository;

import com.example.application.model.Consulta;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepository {
    long proximoId();
    List<Consulta> listar();
    Optional<Consulta> buscarPorId(long id);
    void salvar(Consulta consulta);
    void excluir(long id);
}
