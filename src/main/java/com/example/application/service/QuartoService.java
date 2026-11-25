package com.example.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.application.model.Quarto;
import com.example.application.model.SituacaoQuarto;
import com.example.application.repository.QuartoRepository;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;

@Service
public class QuartoService {
    private final QuartoRepository repository;

    public QuartoService(QuartoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Quarto criar(int numero, int andar, int capacidade) {
        return alterar(() -> {
            validarNumeroUnico(numero, null);
            Quarto quarto = new Quarto(repository.proximoId(), numero, andar, capacidade);
            repository.salvar(quarto);
            return quarto;
        });
    }

    public List<Quarto> listar(Integer numero, Integer andar, SituacaoQuarto situacao) {
        Predicate<Quarto> filtro = quarto -> (numero == null || quarto.getNumero() == numero)
            && (andar == null || quarto.getAndar() == andar)
            && (situacao == null || quarto.getSituacao() == situacao);
        return consultar(filtro, Comparator.comparingInt(quarto -> quarto.getNumero()));
    }

    public List<Quarto> consultar(Predicate<Quarto> filtro, Comparator<Quarto> ordem) {
        return repository.listar().stream().filter(filtro).sorted(ordem).toList();
    }

    public Quarto buscar(long id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new NaoEncontradoException("Quarto não encontrado."));
    }

    @Transactional
    public Quarto atualizar(long id, int numero, int andar, int capacidade) {
        return alterar(() -> {
            Quarto quarto = buscar(id);
            validarNumeroUnico(numero, id);
            quarto.atualizar(numero, andar, capacidade);
            repository.salvar(quarto);
            return quarto;
        });
    }

    @Transactional
    public void excluir(long id) {
        alterar(() -> {
            Quarto quarto = buscar(id);
            if (quarto.ocupacaoAtual() > 0) {
                throw new ConflitoException("Não é possível excluir um quarto com internações ativas.");
            }
            repository.excluir(id);
            return null;
        });
    }

    @Transactional
    public Quarto registrarEntrada(long quartoId, long internacaoId) {
        return alterar(() -> {
            Quarto quarto = buscar(quartoId);
            if (repository.listar().stream().anyMatch(q -> q.getInternacoesAtivas().contains(internacaoId))) {
                throw new ConflitoException("Internação já está alocada em um quarto.");
            }
            quarto.alocarPaciente(internacaoId);
            repository.salvar(quarto);
            return quarto;
        });
    }

    @Transactional
    public Quarto registrarAlta(long quartoId, long internacaoId) {
        return alterar(() -> {
            Quarto quarto = buscar(quartoId);
            quarto.liberarQuarto(internacaoId);
            repository.salvar(quarto);
            return quarto;
        });
    }

    @Transactional
    public Quarto transferirOcupacao(long origemId, long destinoId, long internacaoId) {
        if (origemId == destinoId) {
            throw new ConflitoException("A internação já está neste quarto.");
        }
        return alterar(() -> {
            Quarto origem = buscar(origemId);
            Quarto destino = buscar(destinoId);
            destino.alocarPaciente(internacaoId);
            origem.liberarQuarto(internacaoId);
            repository.salvar(origem);
            repository.salvar(destino);
            return destino;
        });
    }

    <T> T alterar(Supplier<T> acao) {
        repository.bloquearAlteracoes();
        synchronized (this) {
            return acao.get();
        }
    }

    private void validarNumeroUnico(int numero, Long idAtual) {
        if (repository.listar().stream().anyMatch(q -> q.getNumero() == numero
                && (idAtual == null || q.getId() != idAtual))) {
            throw new ConflitoException("Já existe um quarto com esse número.");
        }
    }
}
