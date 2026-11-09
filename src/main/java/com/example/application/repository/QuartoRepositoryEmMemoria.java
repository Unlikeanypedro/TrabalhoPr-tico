package com.example.application.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.example.application.model.Quarto;

@Repository
public class QuartoRepositoryEmMemoria implements QuartoRepository {
    private final Map<Long, Quarto> quartos = new HashMap<>();
    private long sequencia;

    public synchronized long proximoId() { return ++sequencia; }
    public synchronized List<Quarto> listar() {
        return quartos.values().stream().map(quarto -> quarto.copia()).toList();
    }
    public synchronized Optional<Quarto> buscarPorId(long id) {
        return Optional.ofNullable(quartos.get(id)).map(quarto -> quarto.copia());
    }
    public synchronized void salvar(Quarto quarto) { quartos.put(quarto.getId(), quarto.copia()); }
    public synchronized void excluir(long id) { quartos.remove(id); }
}
