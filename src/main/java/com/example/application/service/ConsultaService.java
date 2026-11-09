package com.example.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.model.*;
import com.example.application.repository.ConsultaRepository;

@Service
public class ConsultaService {
    private final ConsultaRepository repository;
    private final ReferenciasConsulta referencias;

    public ConsultaService(ConsultaRepository repository, ReferenciasConsulta referencias) {
        this.repository = repository;
        this.referencias = referencias;
    }

    public synchronized ConsultaRotina agendarRotina(LocalDateTime dataHora, long pacienteId,
            long medicoId, String motivo) {
        MedicoConsulta medico = validarVinculos(pacienteId, medicoId);
        var consulta = new ConsultaRotina(repository.proximoId(), dataHora, pacienteId, medico, motivo);
        repository.salvar(consulta);
        return consulta;
    }

    public synchronized ConsultaEspecializada agendarEspecializada(LocalDateTime dataHora,
            long pacienteId, long medicoId, String motivo, String especialidade) {
        MedicoConsulta medico = validarVinculos(pacienteId, medicoId);
        var consulta = new ConsultaEspecializada(repository.proximoId(), dataHora, pacienteId,
            medico, motivo, especialidade);
        repository.salvar(consulta);
        return consulta;
    }

    public synchronized AtendimentoEmergencia registrarEmergencia(LocalDateTime dataHora,
            long pacienteId, long medicoId, String motivo, PrioridadeAtendimento prioridade) {
        MedicoConsulta medico = validarVinculos(pacienteId, medicoId);
        var consulta = new AtendimentoEmergencia(repository.proximoId(), dataHora, pacienteId,
            medico, motivo, prioridade);
        repository.salvar(consulta);
        return consulta;
    }

    public synchronized Consulta buscar(long id) {
        validarId(id, "consulta");
        return repository.buscarPorId(id)
            .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada."));
    }

    public synchronized List<Consulta> listar(LocalDate data, Long medicoId,
            TipoConsulta tipo, SituacaoConsulta situacao) {
        if (medicoId != null) validarId(medicoId, "médico");
        return repository.listar().stream()
            .filter(c -> data == null || c.getDataHora().toLocalDate().equals(data))
            .filter(c -> medicoId == null || c.getMedico().id() == medicoId)
            .filter(c -> tipo == null || c.getTipo() == tipo)
            .filter(c -> situacao == null || c.getSituacao() == situacao)
            .sorted(Comparator.comparing(Consulta::getDataHora).thenComparingLong(Consulta::getId))
            .toList();
    }

    public synchronized Consulta atualizar(long id, LocalDateTime dataHora, long pacienteId,
            long medicoId, String motivo) {
        Consulta consulta = buscar(id);
        MedicoConsulta medico = validarVinculos(pacienteId, medicoId);
        consulta.atualizarAgendamento(dataHora, pacienteId, medico, motivo);
        repository.salvar(consulta);
        return consulta;
    }

    public synchronized Consulta alterarSituacao(long id, SituacaoConsulta situacao) {
        Consulta consulta = buscar(id);
        if (situacao == SituacaoConsulta.EM_ATENDIMENTO) {
            MedicoConsulta medico = validarVinculos(consulta.getPacienteId(), consulta.getMedico().id());
            consulta.atualizarAgendamento(consulta.getDataHora(), consulta.getPacienteId(), medico,
                consulta.getMotivo());
        }
        consulta.alterarSituacao(situacao);
        repository.salvar(consulta);
        return consulta;
    }

    public synchronized Consulta registrarObservacoes(long id, String observacoes) {
        Consulta consulta = buscar(id);
        consulta.registrarObservacoes(observacoes);
        repository.salvar(consulta);
        return consulta;
    }

    public synchronized void excluir(long id) {
        Consulta consulta = buscar(id);
        if (consulta.getSituacao() == SituacaoConsulta.EM_ATENDIMENTO
                || consulta.getSituacao() == SituacaoConsulta.CONCLUIDA)
            throw new ConflitoException("Não é possível excluir consultas em atendimento ou concluídas.");
        repository.excluir(id);
    }

    private MedicoConsulta validarVinculos(long pacienteId, long medicoId) {
        validarId(pacienteId, "paciente");
        validarId(medicoId, "médico");
        if (!referencias.pacienteExiste(pacienteId))
            throw new NaoEncontradoException("Paciente não encontrado.");
        return referencias.buscarMedico(medicoId)
            .orElseThrow(() -> new NaoEncontradoException("Médico não encontrado."));
    }

    private static void validarId(long id, String entidade) {
        if (id <= 0) throw new IllegalArgumentException("ID de " + entidade + " deve ser positivo.");
    }
}
