package com.example.application.model;

import java.time.LocalDateTime;

public final class ConsultaEspecializada extends Consulta {

    private final String especialidade;

    public ConsultaEspecializada(
        long id,
        LocalDateTime dataHora,
        long pacienteId,
        MedicoConsulta medico,
        String motivo,
        String especialidade
    ) {
        super(id, dataHora, pacienteId, medico, motivo);
        this.especialidade = textoObrigatorio(especialidade, "Especialidade");
        validarCondicoesAtendimento();
    }

    @Override
    protected void validarMedico(MedicoConsulta medico) {
        if (
            medico == null ||
            !especialidade.equalsIgnoreCase(medico.especialidade())
        ) throw new IllegalArgumentException(
            "A especialidade do médico deve ser compatível com a consulta."
        );
    }

    @Override
    public void validarCondicoesAtendimento() {
        validarMedico(getMedico());
    }

    public String getEspecialidade() {
        return especialidade;
    }

    @Override
    public TipoConsulta getTipo() {
        return TipoConsulta.ESPECIALIZADA;
    }

    private ConsultaEspecializada(ConsultaEspecializada origem) {
        super(origem);
        this.especialidade = origem.especialidade;
    }

    @Override
    public ConsultaEspecializada copia() {
        return new ConsultaEspecializada(this);
    }
}
