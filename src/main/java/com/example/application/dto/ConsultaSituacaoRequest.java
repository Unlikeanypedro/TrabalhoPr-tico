package com.example.application.dto;

import com.example.application.model.SituacaoConsulta;

public record ConsultaSituacaoRequest(SituacaoConsulta situacao) {
    public void validar() {
        if (situacao == null) throw new IllegalArgumentException(
            "Informe a situação da consulta."
        );
    }
}
