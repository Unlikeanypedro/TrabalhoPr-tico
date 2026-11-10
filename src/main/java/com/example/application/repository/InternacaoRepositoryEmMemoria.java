package com.example.application.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.example.application.model.Internacao;

@Repository
public class InternacaoRepositoryEmMemoria implements InternacaoRepository {
    private final Map<Long, Internacao> internacoes = new HashMap<>();
    private long sequencia;

    public synchronized long proximoId() { return ++sequencia; }
    public synchronized List<Internacao> listar() {
        return internacoes.values().stream().map(internacao -> internacao.copia()).toList();
    }
    public synchronized Optional<Internacao> buscarPorId(long id) {
        return Optional.ofNullable(internacoes.get(id)).map(internacao -> internacao.copia());
    }
    public synchronized void salvar(Internacao internacao) {
        internacoes.put(internacao.getId(), internacao.copia());
    }
}
