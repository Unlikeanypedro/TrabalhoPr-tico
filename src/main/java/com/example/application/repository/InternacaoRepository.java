package com.example.application.repository;

import java.util.List;
import java.util.Optional;
import com.example.application.model.Internacao;

public interface InternacaoRepository {
    long proximoId();
    List<Internacao> listar();
    Optional<Internacao> buscarPorId(long id);
    void salvar(Internacao internacao);
}
