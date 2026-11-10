package com.example.application.dto;

public record ConsultaObservacoesRequest(String observacoes) {
    public void validar() {
        if (
            observacoes == null || observacoes.isBlank()
        ) throw new IllegalArgumentException(
            "Observações devem ser informadas."
        );
    }
}
