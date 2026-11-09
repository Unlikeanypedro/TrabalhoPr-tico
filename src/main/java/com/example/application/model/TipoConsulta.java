package com.example.application.model;

public enum TipoConsulta {
    ROTINA("Rotina"),
    ESPECIALIZADA("Especializada"),
    EMERGENCIA("Emergência");

    private final String descricao;

    TipoConsulta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
