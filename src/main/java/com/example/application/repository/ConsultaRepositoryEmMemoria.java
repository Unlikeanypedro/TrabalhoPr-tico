package com.example.application.repository;

import com.example.application.model.Consulta;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaRepositoryEmMemoria implements ConsultaRepository {

    private final Map<Long, Consulta> consultas = new HashMap<>();
    private long sequencia;

    public synchronized long proximoId() {
        return ++sequencia;
    }

    public synchronized List<Consulta> listar() {
        return consultas.values().stream().map(Consulta::copia).toList();
    }

    public synchronized Optional<Consulta> buscarPorId(long id) {
        return Optional.ofNullable(consultas.get(id)).map(Consulta::copia);
    }

    public synchronized void salvar(Consulta consulta) {
        consultas.put(consulta.getId(), consulta.copia());
        sequencia = Math.max(sequencia, consulta.getId());
    }

    public synchronized void excluir(long id) {
        consultas.remove(id);
    }
}
