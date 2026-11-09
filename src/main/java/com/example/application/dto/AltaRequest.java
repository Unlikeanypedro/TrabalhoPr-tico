package com.example.application.dto;

import java.time.LocalDate;

/** Corpo do PATCH /internacoes/{id}/alta. */
public class AltaRequest {
    private LocalDate dataEfetivaAlta;
    private String observacoes;

    public AltaRequest() {}

    public LocalDate getDataEfetivaAlta() { return dataEfetivaAlta; }
    public void setDataEfetivaAlta(LocalDate dataEfetivaAlta) { this.dataEfetivaAlta = dataEfetivaAlta; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
