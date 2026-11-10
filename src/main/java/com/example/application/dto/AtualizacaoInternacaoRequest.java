package com.example.application.dto;

import java.time.LocalDate;

/** Corpo do PUT /internacoes/{id}: o que pode mudar enquanto a internação está ativa. */
public class AtualizacaoInternacaoRequest {
    private Long profissionalId;
    private LocalDate dataPrevistaAlta;
    private String observacoes;

    public AtualizacaoInternacaoRequest() {}

    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public LocalDate getDataPrevistaAlta() { return dataPrevistaAlta; }
    public void setDataPrevistaAlta(LocalDate dataPrevistaAlta) { this.dataPrevistaAlta = dataPrevistaAlta; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
