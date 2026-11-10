package com.example.application.dto;

import java.time.LocalDate;
import com.example.application.model.Internacao;
import com.example.application.model.SituacaoInternacao;

public class InternacaoResponse {
    private final long id;
    private final long pacienteId;
    private final long profissionalId;
    private final long quartoId;
    private final LocalDate dataEntrada;
    private final LocalDate dataPrevistaAlta;
    private final LocalDate dataEfetivaAlta;
    private final String observacoes;
    private final SituacaoInternacao situacao;

    public InternacaoResponse(long id, long pacienteId, long profissionalId, long quartoId,
            LocalDate dataEntrada, LocalDate dataPrevistaAlta, LocalDate dataEfetivaAlta,
            String observacoes, SituacaoInternacao situacao) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.quartoId = quartoId;
        this.dataEntrada = dataEntrada;
        this.dataPrevistaAlta = dataPrevistaAlta;
        this.dataEfetivaAlta = dataEfetivaAlta;
        this.observacoes = observacoes;
        this.situacao = situacao;
    }

    public long getId() { return id; }
    public long getPacienteId() { return pacienteId; }
    public long getProfissionalId() { return profissionalId; }
    public long getQuartoId() { return quartoId; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public LocalDate getDataPrevistaAlta() { return dataPrevistaAlta; }
    public LocalDate getDataEfetivaAlta() { return dataEfetivaAlta; }
    public String getObservacoes() { return observacoes; }
    public SituacaoInternacao getSituacao() { return situacao; }

    public static InternacaoResponse de(Internacao i) {
        return new InternacaoResponse(i.getId(), i.getPacienteId(), i.getProfissionalId(), i.getQuartoId(),
            i.getDataEntrada(), i.getDataPrevistaAlta(), i.getDataEfetivaAlta(), i.getObservacoes(),
            i.getSituacao());
    }
}
