package com.example.application.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.application.model.Quarto;
import com.example.application.model.SituacaoQuarto;
import com.example.application.repository.QuartoRepository;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;

@Service
public class QuartoService {
    private final QuartoRepository repository;
    public QuartoService(QuartoRepository repository) { this.repository = repository; }

    public synchronized Quarto criar(int numero, int andar, int capacidade) {
        validarNumeroUnico(numero, null);
        Quarto quarto = new Quarto(repository.proximoId(), numero, andar, capacidade);
        repository.salvar(quarto);
        return quarto;
    }

    public synchronized List<Quarto> listar(Integer numero, Integer andar, SituacaoQuarto situacao) {
        return repository.listar().stream()
            .filter(q -> numero == null || q.getNumero() == numero)
            .filter(q -> andar == null || q.getAndar() == andar)
            .filter(q -> situacao == null || q.getSituacao() == situacao)
            .sorted(Comparator.comparingInt((Quarto quarto) -> quarto.getNumero())).toList();
    }

    public synchronized Quarto buscar(long id) {
        return repository.buscarPorId(id)
            .orElseThrow(() -> new NaoEncontradoException("Quarto não encontrado."));
    }

    public synchronized Quarto atualizar(long id, int numero, int andar, int capacidade) {
        Quarto quarto = buscar(id);
        validarNumeroUnico(numero, id);
        quarto.atualizar(numero, andar, capacidade);
        repository.salvar(quarto);
        return quarto;
    }

    public synchronized void excluir(long id) {
        Quarto quarto = buscar(id);
        if (quarto.ocupacaoAtual() > 0)
            throw new ConflitoException("Não é possível excluir um quarto com internações ativas.");
        repository.excluir(id);
    }

    public synchronized Quarto registrarEntrada(long quartoId, long internacaoId) {
        Quarto quarto = buscar(quartoId);
        if (repository.listar().stream().anyMatch(q -> q.getInternacoesAtivas().contains(internacaoId)))
            throw new ConflitoException("Internação já está alocada em um quarto.");
        quarto.alocarPaciente(internacaoId);
        repository.salvar(quarto);
        return quarto;
    }

    public synchronized Quarto registrarAlta(long quartoId, long internacaoId) {
        Quarto quarto = buscar(quartoId);
        quarto.liberarQuarto(internacaoId);
        repository.salvar(quarto);
        return quarto;
    }

    private void validarNumeroUnico(int numero, Long idAtual) {
        if (repository.listar().stream().anyMatch(q -> q.getNumero() == numero
                && (idAtual == null || q.getId() != idAtual)))
            throw new ConflitoException("Já existe um quarto com esse número.");
    }
}
