package com.example.application.model;

import java.time.LocalDateTime;

public final class AtendimentoEmergencia extends Consulta {

    private final PrioridadeAtendimento prioridade;

    public AtendimentoEmergencia(
        long id,
        LocalDateTime dataHora,
        long pacienteId,
        MedicoConsulta medico,
        String motivo,
        PrioridadeAtendimento prioridade
    ) {
        super(id, dataHora, pacienteId, medico, motivo);
        this.prioridade = prioridade;
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
        if (prioridade == null) throw new IllegalArgumentException(
            "Informe a prioridade da emergência."
        );
    }

    public PrioridadeAtendimento getPrioridade() {
        return prioridade;
    }

    public boolean isAguardandoAvaliacao() {
        return (
            getSituacao() == SituacaoConsulta.AGENDADA ||
            getSituacao() == SituacaoConsulta.CONFIRMADA
        );
    }

    @Override
    public TipoConsulta getTipo() {
        return TipoConsulta.EMERGENCIA;
    }

    private AtendimentoEmergencia(AtendimentoEmergencia origem) {
        super(origem);
        this.prioridade = origem.prioridade;
    }

    @Override
    public AtendimentoEmergencia copia() {
        return new AtendimentoEmergencia(this);
    }
}
