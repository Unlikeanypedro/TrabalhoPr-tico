package com.example.application.model;

public record Encaminhamento(long pacienteId, String especialidade, String motivo) {
    public Encaminhamento {
        if (pacienteId <= 0) throw new IllegalArgumentException("ID do paciente deve ser positivo.");
        especialidade = Profissional.texto(especialidade, "Especialidade");
        motivo = Profissional.texto(motivo, "Motivo do encaminhamento");
    }
}
