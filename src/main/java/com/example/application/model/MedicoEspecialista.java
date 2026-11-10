package com.example.application.model;

public final class MedicoEspecialista extends Profissional {
    private final String especialidade;
    public MedicoEspecialista(long id, String nome, String registro, String telefone, String email, String especialidade) {
        super(id, nome, registro, telefone, email);
        this.especialidade = texto(especialidade, "Especialidade");
    }
    @Override public CategoriaProfissional getCategoria() { return CategoriaProfissional.ESPECIALISTA; }
    @Override public String getEspecialidade() { return especialidade; }
}
