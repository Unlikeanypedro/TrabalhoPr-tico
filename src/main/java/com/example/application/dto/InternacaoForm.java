package com.example.application.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Dados do formulário de internação da tela (cadastro e edição). */
public class InternacaoForm {
    private Long id;
    private Long pacienteId;
    private Long profissionalId;
    private Long quartoId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataEntrada;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevistaAlta;
    private String observacoes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
