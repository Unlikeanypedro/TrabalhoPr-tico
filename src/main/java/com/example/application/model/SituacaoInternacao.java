package com.example.application.model;

public enum SituacaoInternacao {
    ATIVA("Ativa"),
    FINALIZADA("Finalizada");

    private final String descricao;

    SituacaoInternacao(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }

    /** Uma internação só pode ir de ATIVA para FINALIZADA (alta). Depois disso não muda mais. */
    public boolean permiteTransicaoPara(SituacaoInternacao destino) {
        return this == ATIVA && destino == FINALIZADA;
    }
}
