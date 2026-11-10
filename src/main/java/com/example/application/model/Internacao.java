package com.example.application.model;

import java.time.LocalDate;
import com.example.application.exception.ConflitoException;
import com.example.application.utils.InternacaoValidacao;

/**
 * Internação de um paciente em um quarto, sob a responsabilidade de um profissional.
 *
 * Regras:
 * - data de entrada obrigatória e não futura;
 * - data prevista de alta não pode ser anterior à entrada;
 * - só internação ATIVA pode ser editada, transferida ou receber alta;
 * - a alta não pode ser antes da entrada nem futura;
 * - depois da alta (FINALIZADA) a internação não muda mais.
 * A regra de vagas fica no Quarto (alocarPaciente / liberarQuarto).
 */
public class Internacao {
    private final long id;
    private final long pacienteId;
    private long profissionalId;
    private long quartoId;
    private final LocalDate dataEntrada;
    private LocalDate dataPrevistaAlta;
    private LocalDate dataEfetivaAlta;
    private String observacoes;
    private SituacaoInternacao situacao = SituacaoInternacao.ATIVA;

    public Internacao(long id, Long pacienteId, Long profissionalId, Long quartoId,
            LocalDate dataEntrada, LocalDate dataPrevistaAlta, String observacoes) {
        if (id <= 0) throw new IllegalArgumentException("ID da internação deve ser positivo.");
        InternacaoValidacao.validarIdentificador(pacienteId, "paciente");
        InternacaoValidacao.validarIdentificador(profissionalId, "profissional responsável");
        InternacaoValidacao.validarIdentificador(quartoId, "quarto");
        InternacaoValidacao.validarDataEntrada(dataEntrada);
        InternacaoValidacao.validarDataPrevistaAlta(dataEntrada, dataPrevistaAlta);
        this.id = id;
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.quartoId = quartoId;
        this.dataEntrada = dataEntrada;
        this.dataPrevistaAlta = dataPrevistaAlta;
        this.observacoes = InternacaoValidacao.normalizarObservacoes(observacoes);
    }

    private Internacao(Internacao origem) {
        this.id = origem.id;
        this.pacienteId = origem.pacienteId;
        this.profissionalId = origem.profissionalId;
        this.quartoId = origem.quartoId;
        this.dataEntrada = origem.dataEntrada;
        this.dataPrevistaAlta = origem.dataPrevistaAlta;
        this.dataEfetivaAlta = origem.dataEfetivaAlta;
        this.observacoes = origem.observacoes;
        this.situacao = origem.situacao;
    }

    public Internacao copia() { return new Internacao(this); }

    /** Muda o profissional responsável, a data prevista de alta (prorrogar ou antecipar) e as observações. */
    public void atualizar(Long profissionalId, LocalDate dataPrevistaAlta, String observacoes) {
        exigirAtiva("editar");
        InternacaoValidacao.validarIdentificador(profissionalId, "profissional responsável");
        InternacaoValidacao.validarDataPrevistaAlta(dataEntrada, dataPrevistaAlta);
        String texto = InternacaoValidacao.normalizarObservacoes(observacoes);
        this.profissionalId = profissionalId;
        this.dataPrevistaAlta = dataPrevistaAlta;
        this.observacoes = texto;
    }

    /** Troca o quarto da internação. A vaga em si é controlada pelo QuartoService. */
    public void transferirPara(Long novoQuartoId) {
        exigirAtiva("transferir");
        InternacaoValidacao.validarIdentificador(novoQuartoId, "quarto de destino");
        if (novoQuartoId == quartoId)
            throw new ConflitoException("A internação já está neste quarto.");
        this.quartoId = novoQuartoId;
    }

    /** Registra a alta. A liberação da vaga no quarto é feita pelo QuartoService. */
    public void darAlta(LocalDate dataAlta, String observacaoAlta) {
        if (!situacao.permiteTransicaoPara(SituacaoInternacao.FINALIZADA))
            throw new ConflitoException("Esta internação já recebeu alta.");
        InternacaoValidacao.validarDataAlta(dataEntrada, dataAlta);
        String texto = InternacaoValidacao.normalizarObservacoes(observacaoAlta);
        if (!texto.isEmpty()) {
            String linha = "Alta: " + texto;
            this.observacoes = InternacaoValidacao.normalizarObservacoes(
                observacoes.isEmpty() ? linha : observacoes + "\n" + linha);
        }
        this.dataEfetivaAlta = dataAlta;
        this.situacao = SituacaoInternacao.FINALIZADA;
    }

    public boolean estaAtiva() { return situacao == SituacaoInternacao.ATIVA; }

    private void exigirAtiva(String acao) {
        if (!estaAtiva())
            throw new ConflitoException("Não é possível " + acao + " uma internação finalizada.");
    }

    public long getId() { return id; }
    public long getPacienteId() { return pacienteId; }
    public long getProfissionalId() { return profissionalId; }
    public long getQuartoId() { return quartoId; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public LocalDate getDataPrevistaAlta() { return dataPrevistaAlta; }
    public LocalDate getDataEfetivaAlta() { return dataEfetivaAlta; }
    public String getObservacoes() { return observacoes; }
    public SituacaoInternacao getSituacao() { return situacao; }
}
