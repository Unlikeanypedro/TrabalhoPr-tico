package com.example.application.model;

import com.example.application.exception.ConflitoException;
import java.time.LocalDateTime;

public abstract class Consulta {

    private final long id;
    private LocalDateTime dataHora;
    private long pacienteId;
    private MedicoConsulta medico;
    private String motivo;
    private String observacoes = "";
    private SituacaoConsulta situacao = SituacaoConsulta.AGENDADA;

    protected Consulta(
        long id,
        LocalDateTime dataHora,
        long pacienteId,
        MedicoConsulta medico,
        String motivo
    ) {
        if (id <= 0) throw new IllegalArgumentException(
            "ID da consulta deve ser positivo."
        );
        validarDados(dataHora, pacienteId, medico, motivo);
        this.id = id;
        this.dataHora = dataHora;
        this.pacienteId = pacienteId;
        this.medico = medico;
        this.motivo = motivo.strip();
    }

    public final void atualizarAgendamento(
        LocalDateTime dataHora,
        long pacienteId,
        MedicoConsulta medico,
        String motivo
    ) {
        if (
            situacao != SituacaoConsulta.AGENDADA &&
            situacao != SituacaoConsulta.CONFIRMADA
        ) throw new ConflitoException(
            "Só é possível editar consultas agendadas ou confirmadas."
        );
        validarDados(dataHora, pacienteId, medico, motivo);
        validarMedico(medico);
        this.dataHora = dataHora;
        this.pacienteId = pacienteId;
        this.medico = medico;
        this.motivo = motivo.strip();
    }

    public final void alterarSituacao(SituacaoConsulta destino) {
        if (destino == null) throw new IllegalArgumentException(
            "Informe a situação da consulta."
        );
        if (
            !situacao.permiteTransicaoPara(destino)
        ) throw new ConflitoException(
            "Não é permitido alterar a consulta de " +
                situacao.getDescricao() +
                " para " +
                destino.getDescricao() +
                "."
        );
        if (
            destino == SituacaoConsulta.EM_ATENDIMENTO
        ) validarCondicoesAtendimento();
        situacao = destino;
    }

    public final void registrarObservacoes(String observacoes) {
        if (
            situacao != SituacaoConsulta.EM_ATENDIMENTO &&
            situacao != SituacaoConsulta.CONCLUIDA
        ) throw new ConflitoException(
            "Observações só podem ser registradas durante ou após o atendimento."
        );
        this.observacoes = textoObrigatorio(observacoes, "Observações");
    }

    private static void validarDados(
        LocalDateTime dataHora,
        long pacienteId,
        MedicoConsulta medico,
        String motivo
    ) {
        if (dataHora == null) throw new IllegalArgumentException(
            "Informe a data e o horário da consulta."
        );
        if (pacienteId <= 0) throw new IllegalArgumentException(
            "ID do paciente deve ser positivo."
        );
        if (medico == null) throw new IllegalArgumentException(
            "Informe o médico responsável."
        );
        textoObrigatorio(motivo, "Motivo");
    }

    protected static String textoObrigatorio(String texto, String campo) {
        if (
            texto == null || texto.isBlank()
        ) throw new IllegalArgumentException(campo + " deve ser informado.");
        return texto.strip();
    }

    protected abstract void validarMedico(MedicoConsulta medico);

    public abstract void validarCondicoesAtendimento();

    public abstract TipoConsulta getTipo();

    public long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public long getPacienteId() {
        return pacienteId;
    }

    public MedicoConsulta getMedico() {
        return medico;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public SituacaoConsulta getSituacao() {
        return situacao;
    }
}
