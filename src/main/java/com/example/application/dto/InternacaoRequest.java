package com.example.application.dto;

import java.time.LocalDate;

/** Corpo do POST /internacoes. As regras de negócio são verificadas na classe Internacao. */
public class InternacaoRequest {
    private Long pacienteId;
    private Long profissionalId;
    private Long quartoId;
    private LocalDate dataEntrada;
    private LocalDate dataPrevistaAlta;
    private String observacoes;

    public InternacaoRequest() {}

    public InternacaoRequest(Long pacienteId, Long profissionalId, Long quartoId,
            LocalDate dataEntrada, LocalDate dataPrevistaAlta, String observacoes) {
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.quartoId = quartoId;
        this.dataEntrada = dataEntrada;
        this.dataPrevistaAlta = dataPrevistaAlta;
        this.observacoes = observacoes;
    }

    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public Long getQuartoId() { return quartoId; }
    public void setQuartoId(Long quartoId) { this.quartoId = quartoId; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDate dataEntrada) { this.dataEntrada = dataEntrada; }
    public LocalDate getDataPrevistaAlta() { return dataPrevistaAlta; }
    public void setDataPrevistaAlta(LocalDate dataPrevistaAlta) { this.dataPrevistaAlta = dataPrevistaAlta; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
