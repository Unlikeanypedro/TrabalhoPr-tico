package com.example.application.dto;

import com.example.application.model.Quarto;
import com.example.application.model.SituacaoQuarto;

public class QuartoResponse {
    private final long id;
    private final int numero;
    private final int andar;
    private final int capacidadeMaxima;
    private final int ocupacaoAtual;
    private final SituacaoQuarto situacao;

    public QuartoResponse(long id, int numero, int andar, int capacidadeMaxima,
                          int ocupacaoAtual, SituacaoQuarto situacao) {
        this.id = id;
        this.numero = numero;
        this.andar = andar;
        this.capacidadeMaxima = capacidadeMaxima;
        this.ocupacaoAtual = ocupacaoAtual;
        this.situacao = situacao;
    }

    public long getId() { return id; }
    public int getNumero() { return numero; }
    public int getAndar() { return andar; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public int getOcupacaoAtual() { return ocupacaoAtual; }
    public SituacaoQuarto getSituacao() { return situacao; }

    public static QuartoResponse de(Quarto q) {
        return new QuartoResponse(q.getId(), q.getNumero(), q.getAndar(),
            q.getCapacidadeMaxima(), q.ocupacaoAtual(), q.getSituacao());
    }
}
