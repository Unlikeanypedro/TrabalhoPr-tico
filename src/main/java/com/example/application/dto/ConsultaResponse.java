package com.example.application.dto;

import com.example.application.model.*;
import java.time.LocalDateTime;

public record ConsultaResponse(
    long id,
    TipoConsulta tipo,
    LocalDateTime dataHora,
    long pacienteId,
    long medicoId,
    String motivo,
    String observacoes,
    SituacaoConsulta situacao,
    String especialidade,
    PrioridadeAtendimento prioridade
) {
    public static ConsultaResponse de(Consulta consulta) {
        return new ConsultaResponse(
            consulta.getId(),
            consulta.getTipo(),
            consulta.getDataHora(),
            consulta.getPacienteId(),
            consulta.getMedico().id(),
            consulta.getMotivo(),
            consulta.getObservacoes(),
            consulta.getSituacao(),
            consulta instanceof ConsultaEspecializada especializada
                ? especializada.getEspecialidade()
                : null,
            consulta instanceof AtendimentoEmergencia emergencia
                ? emergencia.getPrioridade()
                : null
        );
    }
}
