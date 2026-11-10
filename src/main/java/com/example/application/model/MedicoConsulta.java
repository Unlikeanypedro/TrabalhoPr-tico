package com.example.application.model;

public record MedicoConsulta(long id, String especialidade) {
    public MedicoConsulta {
        if (id <= 0) throw new IllegalArgumentException(
            "ID do médico deve ser positivo."
        );
        if (especialidade != null) {
            especialidade = especialidade.strip();
            if (especialidade.isEmpty()) throw new IllegalArgumentException(
                "Especialidade do médico não pode estar em branco."
            );
        }
    }
}
