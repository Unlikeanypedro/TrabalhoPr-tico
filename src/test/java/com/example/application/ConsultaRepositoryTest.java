package com.example.application;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import com.example.application.model.*;
import com.example.application.repository.ConsultaRepositoryEmMemoria;

class ConsultaRepositoryTest {
    static Stream<Consulta> modalidades() {
        var horario = LocalDateTime.of(2026, 11, 20, 10, 30);
        var medico = new MedicoConsulta(1, "Cardiologia");
        return Stream.of(
            new ConsultaRotina(1, horario, 10, medico, "Avaliação"),
            new ConsultaEspecializada(2, horario, 10, medico, "Retorno", "Cardiologia"),
            new AtendimentoEmergencia(3, horario, 10, medico, "Dor", PrioridadeAtendimento.CRITICA)
        );
    }

    @ParameterizedTest
    @MethodSource("modalidades")
    void salvaCopiaCompletaDeCadaModalidadeSemCompartilharEstado(Consulta consulta) {
        var repository = new ConsultaRepositoryEmMemoria();
        consulta.alterarSituacao(SituacaoConsulta.EM_ATENDIMENTO);
        consulta.registrarObservacoes("Avaliação registrada");
        repository.salvar(consulta);
        consulta.registrarObservacoes("Alteração externa");
        var salva = repository.buscarPorId(consulta.getId()).orElseThrow();
        assertNotSame(consulta, salva);
        assertEquals(consulta.getClass(), salva.getClass());
        assertEquals(consulta.getId(), salva.getId());
        assertEquals(consulta.getDataHora(), salva.getDataHora());
        assertEquals(consulta.getPacienteId(), salva.getPacienteId());
        assertEquals(consulta.getMedico(), salva.getMedico());
        assertEquals(consulta.getMotivo(), salva.getMotivo());
        assertEquals(SituacaoConsulta.EM_ATENDIMENTO, salva.getSituacao());
        assertEquals("Avaliação registrada", salva.getObservacoes());
        if (salva instanceof ConsultaEspecializada especializada)
            assertEquals("Cardiologia", especializada.getEspecialidade());
        if (salva instanceof AtendimentoEmergencia emergencia)
            assertEquals(PrioridadeAtendimento.CRITICA, emergencia.getPrioridade());
        salva.alterarSituacao(SituacaoConsulta.CONCLUIDA);
        repository.listar().getFirst().alterarSituacao(SituacaoConsulta.CANCELADA);
        assertEquals(SituacaoConsulta.EM_ATENDIMENTO,
            repository.buscarPorId(consulta.getId()).orElseThrow().getSituacao());
        repository.excluir(consulta.getId());
        assertTrue(repository.buscarPorId(consulta.getId()).isEmpty());
        assertTrue(repository.listar().isEmpty());
    }

    @Test void sequenciaNaoReutilizaIdentificadoresSalvosOuExcluidos() {
        var repository = new ConsultaRepositoryEmMemoria();
        assertEquals(1, repository.proximoId());
        var consulta = new ConsultaRotina(100, LocalDateTime.of(2026, 11, 20, 10, 30),
            10, new MedicoConsulta(1, null), "Avaliação");
        repository.salvar(consulta);
        repository.excluir(100);
        assertEquals(101, repository.proximoId());
    }
}
