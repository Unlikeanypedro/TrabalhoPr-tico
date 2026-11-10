package com.example.application.service;

import com.example.application.dto.ProfissionalRequest;
import com.example.application.exception.*;
import com.example.application.model.*;
import com.example.application.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ProfissionalService {
    private final ProfissionalRepository repository;
    private final ReferenciasConsulta referencias;
    private final ConsultaRepository consultas;
    private final Map<Long, List<Encaminhamento>> encaminhamentos = new HashMap<>();
    private final Map<Long, List<ProcedimentoProfissional>> procedimentos = new HashMap<>();

    public ProfissionalService(ProfissionalRepository repository, ReferenciasConsulta referencias, ConsultaRepository consultas) {
        this.repository = repository;
        this.referencias = referencias;
        this.consultas = consultas;
    }

    public synchronized Profissional criar(ProfissionalRequest dados) {
        Profissional profissional = dados.criar(repository.proximoId());
        validarRegistroUnico(profissional);
        repository.salvar(profissional);
        return profissional;
    }

    public synchronized Profissional buscar(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID do profissional deve ser positivo.");
        return repository.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Profissional não encontrado."));
    }

    public synchronized List<Profissional> listar(String nome, CategoriaProfissional categoria, String especialidade) {
        return repository.listar().stream()
            .filter(p -> nome == null || p.getNome().toLowerCase(Locale.ROOT).contains(nome.strip().toLowerCase(Locale.ROOT)))
            .filter(p -> categoria == null || p.getCategoria() == categoria)
            .filter(p -> especialidade == null || especialidade.isBlank() ||
                p.getEspecialidade() != null && p.getEspecialidade().equalsIgnoreCase(especialidade.strip()))
            .sorted(Comparator.comparing(Profissional::getNome, String.CASE_INSENSITIVE_ORDER).thenComparingLong(Profissional::getId))
            .toList();
    }

    public synchronized Profissional atualizar(long id, ProfissionalRequest dados) {
        Profissional anterior = buscar(id);
        Profissional atualizado = dados.criar(id);
        validarRegistroUnico(atualizado);
        if ((temHistorico(id) || !listarConsultas(id).isEmpty()) &&
                (anterior.getCategoria() != atualizado.getCategoria() ||
                 !Objects.equals(anterior.getEspecialidade(), atualizado.getEspecialidade())))
            throw new ConflitoException("Não é possível alterar categoria ou especialidade de um profissional com registros vinculados.");
        repository.salvar(atualizado);
        return atualizado;
    }

    public synchronized void excluir(long id) {
        buscar(id);
        if (temHistorico(id) || !listarConsultas(id).isEmpty())
            throw new ConflitoException("Não é possível excluir um profissional com registros vinculados.");
        repository.excluir(id);
    }

    public synchronized List<Consulta> listarConsultas(long id) {
        buscar(id);
        return consultas.listar().stream().filter(c -> c.getMedico().id() == id)
            .sorted(Comparator.comparing(Consulta::getDataHora)).toList();
    }

    public synchronized Encaminhamento encaminhar(long id, Encaminhamento dados) {
        if (!(buscar(id) instanceof ClinicoGeral clinico))
            throw new ConflitoException("Somente clínicos gerais podem registrar encaminhamentos neste módulo.");
        validarPaciente(dados.pacienteId());
        Encaminhamento registro = clinico.encaminhar(dados.pacienteId(), dados.especialidade(), dados.motivo());
        encaminhamentos.computeIfAbsent(id, chave -> new ArrayList<>()).add(registro);
        return registro;
    }

    public synchronized ProcedimentoProfissional registrarProcedimento(long id, ProcedimentoProfissional dados) {
        if (!(buscar(id) instanceof Cirurgiao cirurgiao))
            throw new ConflitoException("Somente cirurgiões podem registrar procedimentos cirúrgicos.");
        validarPaciente(dados.pacienteId());
        var registro = cirurgiao.registrarProcedimento(dados.pacienteId(), dados.dataHora(), dados.descricao());
        procedimentos.computeIfAbsent(id, chave -> new ArrayList<>()).add(registro);
        return registro;
    }

    public synchronized List<Encaminhamento> listarEncaminhamentos(long id) {
        buscar(id);
        return List.copyOf(encaminhamentos.getOrDefault(id, List.of()));
    }

    public synchronized List<ProcedimentoProfissional> listarProcedimentos(long id) {
        buscar(id);
        return List.copyOf(procedimentos.getOrDefault(id, List.of()));
    }

    private boolean temHistorico(long id) {
        return !encaminhamentos.getOrDefault(id, List.of()).isEmpty() || !procedimentos.getOrDefault(id, List.of()).isEmpty();
    }

    private void validarPaciente(long id) {
        if (!referencias.pacienteExiste(id)) throw new NaoEncontradoException("Paciente não encontrado.");
    }

    private void validarRegistroUnico(Profissional profissional) {
        if (repository.listar().stream().anyMatch(p -> p.getId() != profissional.getId() &&
                p.getRegistroProfissional().equalsIgnoreCase(profissional.getRegistroProfissional())))
            throw new ConflitoException("Já existe um profissional com esse registro.");
    }
}
