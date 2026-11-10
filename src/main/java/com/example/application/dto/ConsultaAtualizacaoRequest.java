package com.example.application.dto;

import java.time.LocalDateTime;

public record ConsultaAtualizacaoRequest(
    LocalDateTime dataHora,
    Long pacienteId,
    Long medicoId,
    String motivo
) {
    public void validar() {
        if (dataHora == null) throw new IllegalArgumentException(
            "Informe a data e o horário da consulta."
        );
        if (
            pacienteId == null || pacienteId <= 0
        ) throw new IllegalArgumentException(
            "ID do paciente deve ser positivo."
        );
        if (
            medicoId == null || medicoId <= 0
        ) throw new IllegalArgumentException("ID do médico deve ser positivo.");
        if (
            motivo == null || motivo.isBlank()
        ) throw new IllegalArgumentException("Motivo deve ser informado.");
    }
}
