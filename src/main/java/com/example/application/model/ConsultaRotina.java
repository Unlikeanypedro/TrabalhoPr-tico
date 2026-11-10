package com.example.application.model;

import java.time.LocalDateTime;

public final class ConsultaRotina extends Consulta {

    public ConsultaRotina(
        long id,
        LocalDateTime dataHora,
        long pacienteId,
        MedicoConsulta medico,
        String motivo
    ) {
        super(id, dataHora, pacienteId, medico, motivo);
        validarCondicoesAtendimento();
    }

    @Override
    protected void validarMedico(MedicoConsulta medico) {
        if (medico == null) throw new IllegalArgumentException(
            "Informe o médico responsável."
        );
    }

    @Override
    public void validarCondicoesAtendimento() {
        validarMedico(getMedico());
        textoObrigatorio(getMotivo(), "Motivo");
    }

    @Override
    public TipoConsulta getTipo() {
        return TipoConsulta.ROTINA;
    }

    private ConsultaRotina(ConsultaRotina origem) {
        super(origem);
    }

    @Override
    public ConsultaRotina copia() {
        return new ConsultaRotina(this);
    }
}
