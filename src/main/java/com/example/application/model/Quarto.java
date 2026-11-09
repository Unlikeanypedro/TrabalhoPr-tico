package com.example.application.model;

import java.util.HashSet;
import java.util.Set;
import com.example.application.exception.ConflitoException;
import com.example.application.utils.QuartoValidacao;

public class Quarto {
    private final long id;
    private int numero;
    private int andar;
    private int capacidadeMaxima;
    private final Set<Long> internacoesAtivas = new HashSet<>();

    public Quarto(long id, int numero, int andar, int capacidadeMaxima) {
        QuartoValidacao.validarIdentificador(id, "ID");
        this.id = id;
        atualizar(numero, andar, capacidadeMaxima);
    }

    private Quarto(Quarto origem) {
        this.id = origem.id;
        this.numero = origem.numero;
        this.andar = origem.andar;
        this.capacidadeMaxima = origem.capacidadeMaxima;
        this.internacoesAtivas.addAll(origem.internacoesAtivas);
    }

    public Quarto copia() { return new Quarto(this); }

    public void atualizar(int numero, int andar, int capacidadeMaxima) {
        QuartoValidacao.validarDados(numero, andar, capacidadeMaxima);
        if (capacidadeMaxima < ocupacaoAtual())
            throw new ConflitoException("Capacidade não pode ser menor que a ocupação atual.");
        this.numero = numero;
        this.andar = andar;
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public void alocarPaciente(long internacaoId) {
        validarInternacao(internacaoId);
        if (internacoesAtivas.contains(internacaoId))
            throw new ConflitoException("Internação já ocupa este quarto.");
        if (!estaDisponivel()) throw new ConflitoException("Quarto sem vagas disponíveis.");
        internacoesAtivas.add(internacaoId);
    }

    public void liberarQuarto(long internacaoId) {
        validarInternacao(internacaoId);
        if (!internacoesAtivas.remove(internacaoId))
            throw new ConflitoException("Internação não ocupa este quarto.");
    }

    private void validarInternacao(long id) {
        QuartoValidacao.validarIdentificador(id, "ID da internação");
    }

    public long getId() { return id; }
    public int getNumero() { return numero; }
    public int getAndar() { return andar; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public int ocupacaoAtual() { return internacoesAtivas.size(); }
    public boolean estaDisponivel() { return ocupacaoAtual() < capacidadeMaxima; }
    public SituacaoQuarto getSituacao() {
        return estaDisponivel() ? SituacaoQuarto.DISPONIVEL : SituacaoQuarto.OCUPADO;
    }
    public Set<Long> getInternacoesAtivas() { return Set.copyOf(internacoesAtivas); }
}
