package com.example.application.dto;

/** Corpo do PATCH /internacoes/{id}/transferencia. */
public class TransferenciaRequest {
    private Long quartoId;

    public TransferenciaRequest() {}

    public Long getQuartoId() { return quartoId; }
    public void setQuartoId(Long quartoId) { this.quartoId = quartoId; }
}
