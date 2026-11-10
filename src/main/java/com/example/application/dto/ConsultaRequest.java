package com.example.application.dto;

import com.example.application.model.PrioridadeAtendimento;
import com.example.application.model.TipoConsulta;
import java.time.LocalDateTime;

public record ConsultaRequest(
    TipoConsulta tipo,
    LocalDateTime dataHora,
    Long pacienteId,
    Long medicoId,
    String motivo,
    String especialidade,
    PrioridadeAtendimento prioridade
) {
    public void validar() {
        new ConsultaAtualizacaoRequest(
            dataHora,
            pacienteId,
            medicoId,
            motivo
        ).validar();
        if (tipo == null) throw new IllegalArgumentException(
            "Informe a modalidade da consulta."
        );
        if (
            tipo == TipoConsulta.ESPECIALIZADA &&
            (especialidade == null || especialidade.isBlank())
        ) throw new IllegalArgumentException(
            "Especialidade deve ser informada."
        );
        if (
            tipo != TipoConsulta.ESPECIALIZADA && especialidade != null
        ) throw new IllegalArgumentException(
            "Especialidade só deve ser informada para consultas especializadas."
        );
        if (
            tipo == TipoConsulta.EMERGENCIA && prioridade == null
        ) throw new IllegalArgumentException(
            "Informe a prioridade da emergência."
        );
        if (
            tipo != TipoConsulta.EMERGENCIA && prioridade != null
        ) throw new IllegalArgumentException(
            "Prioridade só deve ser informada para emergências."
        );
    }
}
