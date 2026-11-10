package com.example.application.repository;

import com.example.application.model.Profissional;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ProfissionalRepositoryEmMemoria implements ProfissionalRepository {
    private final Map<Long, Profissional> profissionais = new LinkedHashMap<>();
    private long sequencia;
    public synchronized long proximoId() { return ++sequencia; }
    public synchronized void salvar(Profissional profissional) { profissionais.put(profissional.getId(), profissional); }
    public synchronized Optional<Profissional> buscarPorId(long id) { return Optional.ofNullable(profissionais.get(id)); }
    public synchronized List<Profissional> listar() { return List.copyOf(profissionais.values()); }
    public synchronized void excluir(long id) { profissionais.remove(id); }
}
