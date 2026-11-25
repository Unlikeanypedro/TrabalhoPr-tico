package com.example.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.model.Internacao;
import com.example.application.model.Quarto;
import com.example.application.model.SituacaoInternacao;
import com.example.application.repository.InternacaoRepository;
import com.example.application.repository.PacienteRepository;
import com.example.application.repository.ProfissionalRepository;

/**
 * Casos de uso de internação. A ocupação dos quartos é sempre feita pelo
 * QuartoService (registrarEntrada / registrarAlta), assim a regra de vagas fica num lugar só.
 */
@Service
public class InternacaoService {
    private final InternacaoRepository repository;
    private final QuartoService quartoService;
    private final PacienteRepository pacientes;
    private final ProfissionalRepository profissionais;

    public InternacaoService(InternacaoRepository repository, QuartoService quartoService,
            PacienteRepository pacientes, ProfissionalRepository profissionais) {
        this.repository = repository;
        this.quartoService = quartoService;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
    }

    @Transactional
    public Internacao registrar(Long pacienteId, Long profissionalId, Long quartoId,
            LocalDate dataEntrada, LocalDate dataPrevistaAlta, String observacoes) {
        return quartoService.alterar(() -> {
            Internacao internacao = new Internacao(repository.proximoId(), pacienteId, profissionalId,
                quartoId, dataEntrada, dataPrevistaAlta, observacoes);
            if (pacientes.buscarPorId(pacienteId).isEmpty()) {
                throw new NaoEncontradoException("Paciente não encontrado.");
            }
            validarProfissional(profissionalId);
            if (possuiInternacaoAtiva(pacienteId)) {
                throw new ConflitoException("O paciente já possui uma internação ativa.");
            }
            Quarto quarto = quartoService.registrarEntrada(quartoId, internacao.getId());
            internacao.registrarEstadia(quarto, dataEntrada);
            repository.salvar(internacao);
            return internacao;
        });
    }

    public List<Internacao> listar(Long pacienteId, Long profissionalId, Long quartoId,
            SituacaoInternacao situacao) {
        Predicate<Internacao> filtro = i -> (pacienteId == null || i.getPacienteId() == pacienteId)
            && (profissionalId == null || i.getProfissionalId() == profissionalId)
            && (quartoId == null || i.getQuartoId() == quartoId)
            && (situacao == null || i.getSituacao() == situacao);
        Comparator<Internacao> ordem = Comparator.comparing((Internacao i) -> i.getDataEntrada())
            .thenComparingLong(i -> i.getId()).reversed();
        return consultar(filtro, ordem);
    }

    public List<Internacao> consultar(Predicate<Internacao> filtro, Comparator<Internacao> ordem) {
        return repository.listar().stream().filter(filtro).sorted(ordem).toList();
    }

    public Internacao buscar(long id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new NaoEncontradoException("Internação não encontrada."));
    }

    @Transactional
    public Internacao atualizar(long id, Long profissionalId, LocalDate dataPrevistaAlta,
            String observacoes) {
        return editar(id, profissionalId, dataPrevistaAlta, observacoes, null);
    }

    @Transactional
    public Internacao transferir(long id, Long novoQuartoId) {
        return quartoService.alterar(() -> {
            Internacao internacao = buscar(id);
            moverDeQuarto(internacao, novoQuartoId);
            repository.salvar(internacao);
            return internacao;
        });
    }

    /**
     * Usado pela tela: altera os dados e, se o quarto mudou, transfere, tudo de uma vez.
     * Se qualquer regra falhar, nada é gravado.
     */
    @Transactional
    public Internacao editar(long id, Long profissionalId, LocalDate dataPrevistaAlta,
            String observacoes, Long quartoId) {
        return quartoService.alterar(() -> {
            Internacao internacao = buscar(id);
            internacao.atualizar(profissionalId, dataPrevistaAlta, observacoes);
            validarProfissional(profissionalId);
            if (quartoId != null && quartoId != internacao.getQuartoId()) {
                moverDeQuarto(internacao, quartoId);
            }
            repository.salvar(internacao);
            return internacao;
        });
    }

    private void moverDeQuarto(Internacao internacao, Long novoQuartoId) {
        long quartoAnterior = internacao.getQuartoId();
        internacao.transferirPara(novoQuartoId);
        Quarto destino = quartoService.transferirOcupacao(quartoAnterior, novoQuartoId, internacao.getId());
        internacao.registrarEstadia(destino, LocalDate.now());
    }

    @Transactional
    public Internacao darAlta(long id, LocalDate dataAlta, String observacoes) {
        return quartoService.alterar(() -> {
            Internacao internacao = buscar(id);
            internacao.darAlta(dataAlta, observacoes);
            quartoService.registrarAlta(internacao.getQuartoId(), id);
            repository.salvar(internacao);
            return internacao;
        });
    }

    private boolean possuiInternacaoAtiva(long pacienteId) {
        return repository.listar().stream()
            .anyMatch(i -> i.getPacienteId() == pacienteId && i.estaAtiva());
    }

    private void validarProfissional(long profissionalId) {
        if (profissionais.buscarPorId(profissionalId).isEmpty()) {
            throw new NaoEncontradoException("Profissional não encontrado.");
        }
    }
}
