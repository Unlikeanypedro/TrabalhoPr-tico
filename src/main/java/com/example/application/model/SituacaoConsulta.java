package com.example.application.model;

public enum SituacaoConsulta {
    AGENDADA("Agendada"),
    CONFIRMADA("Confirmada"),
    EM_ATENDIMENTO("Em atendimento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada"),
    NAO_COMPARECEU("Não compareceu");

    private final String descricao;

    SituacaoConsulta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean permiteTransicaoPara(SituacaoConsulta destino) {
        if (destino == null) return false;
        return switch (this) {
            case AGENDADA -> destino == CONFIRMADA ||
                destino == EM_ATENDIMENTO ||
                destino == CANCELADA ||
                destino == NAO_COMPARECEU;
            case CONFIRMADA -> destino == EM_ATENDIMENTO ||
                destino == CANCELADA ||
                destino == NAO_COMPARECEU;
            case EM_ATENDIMENTO -> destino == CONCLUIDA || destino == CANCELADA;
            case CONCLUIDA, CANCELADA, NAO_COMPARECEU -> false;
        };
    }
}
