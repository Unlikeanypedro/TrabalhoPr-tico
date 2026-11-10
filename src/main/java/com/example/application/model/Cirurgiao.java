package com.example.application.model;

import java.time.LocalDateTime;

public final class Cirurgiao extends Profissional {
    private final String especialidade;
    private final String areaCirurgica;
    public Cirurgiao(long id, String nome, String registro, String telefone, String email,
            String especialidade, String areaCirurgica) {
        super(id, nome, registro, telefone, email);
        this.especialidade = texto(especialidade, "Especialidade");
        this.areaCirurgica = texto(areaCirurgica, "Área de atuação cirúrgica");
    }
    @Override public CategoriaProfissional getCategoria() { return CategoriaProfissional.CIRURGIAO; }
    @Override public String getEspecialidade() { return especialidade; }
    @Override public String getAreaCirurgica() { return areaCirurgica; }

    public ProcedimentoProfissional registrarProcedimento(long pacienteId, LocalDateTime dataHora, String descricao) {
        return new ProcedimentoProfissional(pacienteId, dataHora, descricao);
    }
}
