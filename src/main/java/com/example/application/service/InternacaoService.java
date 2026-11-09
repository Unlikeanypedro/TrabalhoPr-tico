package com.example.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.model.Internacao;
import com.example.application.model.Quarto;
import com.example.application.model.SituacaoInternacao;
import com.example.application.repository.InternacaoRepository;

/**
 * Casos de uso de internação. A ocupação dos quartos é sempre feita pelo
 * QuartoService (registrarEntrada / registrarAlta), assim a regra de vagas fica num lugar só.
 */
@Service
public class InternacaoService {
    private final InternacaoRepository repository;
    private final QuartoService quartoService;

    public InternacaoService(InternacaoRepository repository, QuartoService quartoService) {
        this.repository = repository;
        this.quartoService = quartoService;
    }

    public synchronized Internacao registrar(Long pacienteId, Long profissionalId, Long quartoId,
            LocalDate dataEntrada, LocalDate dataPrevistaAlta, String observacoes) {
        Internacao internacao = new Internacao(repository.proximoId(), pacienteId, profissionalId,
            quartoId, dataEntrada, dataPrevistaAlta, observacoes);
        quartoService.buscar(quartoId);
        if (possuiInternacaoAtiva(pacienteId))
            throw new ConflitoException("O paciente já possui uma internação ativa.");
        quartoService.registrarEntrada(quartoId, internacao.getId());
        repository.salvar(internacao);
        return internacao;
    }

    public synchronized List<Internacao> listar(Long pacienteId, Long profissionalId, Long quartoId,
            SituacaoInternacao situacao) {
        return repository.listar().stream()
            .filter(i -> pacienteId == null || i.getPacienteId() == pacienteId)
            .filter(i -> profissionalId == null || i.getProfissionalId() == profissionalId)
            .filter(i -> quartoId == null || i.getQuartoId() == quartoId)
            .filter(i -> situacao == null || i.getSituacao() == situacao)
            .sorted(Comparator.comparing((Internacao i) -> i.getDataEntrada()).reversed()
                .thenComparing(Comparator.comparingLong((Internacao i) -> i.getId()).reversed()))
            .toList();
    }

    public synchronized Internacao buscar(long id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new NaoEncontradoException("Internação não encontrada."));
    }

    public synchronized Internacao atualizar(long id, Long profissionalId, LocalDate dataPrevistaAlta,
            String observacoes) {
        Internacao internacao = buscar(id);
        internacao.atualizar(profissionalId, dataPrevistaAlta, observacoes);
        repository.salvar(internacao);
        return internacao;
    }

    public synchronized Internacao transferir(long id, Long novoQuartoId) {
        Internacao internacao = buscar(id);
        moverDeQuarto(internacao, novoQuartoId);
        repository.salvar(internacao);
        return internacao;
    }

    /**
     * Usado pela tela: altera os dados e, se o quarto mudou, transfere, tudo de uma vez.
     * Se qualquer regra falhar, nada é gravado.
     */
    public synchronized Internacao editar(long id, Long profissionalId, LocalDate dataPrevistaAlta,
            String observacoes, Long quartoId) {
        Internacao internacao = buscar(id);
        internacao.atualizar(profissionalId, dataPrevistaAlta, observacoes);
        if (quartoId != null && quartoId != internacao.getQuartoId())
            moverDeQuarto(internacao, quartoId);
        repository.salvar(internacao);
        return internacao;
    }

    private void moverDeQuarto(Internacao internacao, Long novoQuartoId) {
        long quartoAnterior = internacao.getQuartoId();
        internacao.transferirPara(novoQuartoId);
        Quarto destino = quartoService.buscar(novoQuartoId);
        if (!destino.estaDisponivel())
            throw new ConflitoException("O quarto " + destino.getNumero() + " não possui vagas.");
        quartoService.registrarAlta(quartoAnterior, internacao.getId());
        try {
            quartoService.registrarEntrada(novoQuartoId, internacao.getId());
        } catch (RuntimeException e) {
            quartoService.registrarEntrada(quartoAnterior, internacao.getId());
            throw e;
        }
    }

    public synchronized Internacao darAlta(long id, LocalDate dataAlta, String observacoes) {
        Internacao internacao = buscar(id);
        internacao.darAlta(dataAlta, observacoes);
        quartoService.registrarAlta(internacao.getQuartoId(), id);
        repository.salvar(internacao);
        return internacao;
    }

    private boolean possuiInternacaoAtiva(long pacienteId) {
        return repository.listar().stream()
            .anyMatch(i -> i.getPacienteId() == pacienteId && i.estaAtiva());
    }
}
