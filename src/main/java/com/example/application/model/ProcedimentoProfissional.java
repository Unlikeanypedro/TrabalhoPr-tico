package com.example.application.model;

import java.time.LocalDateTime;

/** Registro de um procedimento já realizado pelo cirurgião. */
public record ProcedimentoProfissional(long pacienteId, LocalDateTime dataHora, String descricao) {
    public ProcedimentoProfissional {
        if (pacienteId <= 0) throw new IllegalArgumentException("ID do paciente deve ser positivo.");
        if (dataHora == null || dataHora.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Informe a data e o horário de um procedimento já realizado.");
        descricao = Profissional.texto(descricao, "Descrição do procedimento");
    }
}
