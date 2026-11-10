package com.example.application.model;

public final class ClinicoGeral extends Profissional {
    public ClinicoGeral(long id, String nome, String registro, String telefone, String email) {
        super(id, nome, registro, telefone, email);
    }
    @Override public CategoriaProfissional getCategoria() { return CategoriaProfissional.CLINICO_GERAL; }
    @Override public String getEspecialidade() { return null; }

    public Encaminhamento encaminhar(long pacienteId, String especialidade, String motivo) {
        return new Encaminhamento(pacienteId, especialidade, motivo);
    }
}
