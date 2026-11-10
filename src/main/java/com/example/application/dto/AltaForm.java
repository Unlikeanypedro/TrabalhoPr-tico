package com.example.application.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Dados do formulário "Registrar alta" da tela. */
public class AltaForm {
    private Long internacaoId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataEfetivaAlta;
    private String observacoes;

    public Long getInternacaoId() { return internacaoId; }
    public void setInternacaoId(Long internacaoId) { this.internacaoId = internacaoId; }
    public LocalDate getDataEfetivaAlta() { return dataEfetivaAlta; }
    public void setDataEfetivaAlta(LocalDate dataEfetivaAlta) { this.dataEfetivaAlta = dataEfetivaAlta; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
