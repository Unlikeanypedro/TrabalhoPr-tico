package com.example.application;

import static org.junit.jupiter.api.Assertions.*;

import com.example.application.exception.ConflitoException;
import com.example.application.model.*;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConsultaTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(
        2026,
        11,
        20,
        10,
        30
    );
    private static final MedicoConsulta CLINICO = new MedicoConsulta(1, null);
    private static final MedicoConsulta CARDIOLOGISTA = new MedicoConsulta(
        2,
        "Cardiologia"
    );

    private ConsultaRotina rotina() {
        return new ConsultaRotina(1, HORARIO, 10, CLINICO, " Avaliação geral ");
    }

    @Test
    void agendaRotinaComDadosObrigatorios() {
        var consulta = rotina();
        assertEquals(1, consulta.getId());
        assertEquals(HORARIO, consulta.getDataHora());
        assertEquals(10, consulta.getPacienteId());
        assertEquals(CLINICO, consulta.getMedico());
        assertEquals("Avaliação geral", consulta.getMotivo());
        assertEquals("", consulta.getObservacoes());
        assertEquals(TipoConsulta.ROTINA, consulta.getTipo());
        assertEquals(SituacaoConsulta.AGENDADA, consulta.getSituacao());
    }

    @Test
    void rejeitaDadosObrigatoriosInvalidos() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(0, HORARIO, 10, CLINICO, "Motivo")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(-1, HORARIO, 10, CLINICO, "Motivo")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(1, null, 10, CLINICO, "Motivo")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(1, HORARIO, 0, CLINICO, "Motivo")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(1, HORARIO, -1, CLINICO, "Motivo")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(1, HORARIO, 10, null, "Motivo")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(1, HORARIO, 10, CLINICO, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaRotina(1, HORARIO, 10, CLINICO, " \t ")
        );
        assertThrows(IllegalArgumentException.class, () ->
            new MedicoConsulta(0, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new MedicoConsulta(-1, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new MedicoConsulta(1, " ")
        );
    }

    @Test
    void concluiSomenteAposInicioEPermiteObservacoesAposConclusao() {
        var consulta = rotina();
        assertThrows(ConflitoException.class, () ->
            consulta.alterarSituacao(SituacaoConsulta.CONCLUIDA)
        );
        assertEquals(SituacaoConsulta.AGENDADA, consulta.getSituacao());
        assertThrows(ConflitoException.class, () ->
            consulta.registrarObservacoes("Antes do atendimento")
        );
        consulta.alterarSituacao(SituacaoConsulta.CONFIRMADA);
        assertThrows(ConflitoException.class, () ->
            consulta.alterarSituacao(SituacaoConsulta.CONCLUIDA)
        );
        consulta.alterarSituacao(SituacaoConsulta.EM_ATENDIMENTO);
        consulta.registrarObservacoes(" Avaliação realizada ");
        assertEquals("Avaliação realizada", consulta.getObservacoes());
        consulta.alterarSituacao(SituacaoConsulta.CONCLUIDA);
        consulta.registrarObservacoes(
            "Orientações registradas após a consulta"
        );
        assertEquals(
            "Orientações registradas após a consulta",
            consulta.getObservacoes()
        );
        assertThrows(IllegalArgumentException.class, () ->
            consulta.registrarObservacoes(" ")
        );
        assertThrows(IllegalArgumentException.class, () ->
            consulta.registrarObservacoes(null)
        );
        assertEquals(
            "Orientações registradas após a consulta",
            consulta.getObservacoes()
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = SituacaoConsulta.class,
        names = { "CONCLUIDA", "CANCELADA", "NAO_COMPARECEU" }
    )
    void situacoesFinaisExigemNovoAgendamento(SituacaoConsulta terminal) {
        var consulta = rotina();
        if (terminal == SituacaoConsulta.CONCLUIDA) consulta.alterarSituacao(
            SituacaoConsulta.EM_ATENDIMENTO
        );
        consulta.alterarSituacao(terminal);
        for (var destino : SituacaoConsulta.values()) {
            assertThrows(ConflitoException.class, () ->
                consulta.alterarSituacao(destino)
            );
            assertEquals(terminal, consulta.getSituacao());
        }
        assertThrows(ConflitoException.class, () ->
            consulta.atualizarAgendamento(
                HORARIO.plusDays(1),
                20,
                CARDIOLOGISTA,
                "Novo motivo"
            )
        );
        assertEquals(HORARIO, consulta.getDataHora());
        assertEquals(10, consulta.getPacienteId());
        assertEquals(SituacaoConsulta.AGENDADA, rotina().getSituacao());
    }

    @Test
    void emAtendimentoNaoVoltaParaAgendamentoNemRegistraAusencia() {
        var consulta = rotina();
        consulta.alterarSituacao(SituacaoConsulta.EM_ATENDIMENTO);
        for (var destino : Set.of(
            SituacaoConsulta.AGENDADA,
            SituacaoConsulta.CONFIRMADA,
            SituacaoConsulta.NAO_COMPARECEU
        )) {
            assertThrows(ConflitoException.class, () ->
                consulta.alterarSituacao(destino)
            );
        }
        assertThrows(ConflitoException.class, () ->
            consulta.atualizarAgendamento(
                HORARIO.plusDays(1),
                20,
                CARDIOLOGISTA,
                "Novo motivo"
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            consulta.alterarSituacao(null)
        );
        assertEquals(SituacaoConsulta.EM_ATENDIMENTO, consulta.getSituacao());
        consulta.alterarSituacao(SituacaoConsulta.CANCELADA);
        assertThrows(ConflitoException.class, () ->
            consulta.registrarObservacoes("Observação indevida")
        );
    }

    @Test
    void edicaoValidaTodosOsDadosAntesDeAlterarAgendamento() {
        var consulta = rotina();
        assertThrows(IllegalArgumentException.class, () ->
            consulta.atualizarAgendamento(
                HORARIO.plusDays(1),
                20,
                CARDIOLOGISTA,
                " "
            )
        );
        assertEquals(HORARIO, consulta.getDataHora());
        assertEquals(10, consulta.getPacienteId());
        assertEquals(CLINICO, consulta.getMedico());
        consulta.alterarSituacao(SituacaoConsulta.CONFIRMADA);
        consulta.atualizarAgendamento(
            HORARIO.plusDays(1),
            20,
            CARDIOLOGISTA,
            " Retorno "
        );
        assertEquals(HORARIO.plusDays(1), consulta.getDataHora());
        assertEquals(20, consulta.getPacienteId());
        assertEquals(CARDIOLOGISTA, consulta.getMedico());
        assertEquals("Retorno", consulta.getMotivo());
    }

    @Test
    void especializadaExigeMedicoComEspecialidadeCompativelInclusiveNaEdicao() {
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaEspecializada(
                1,
                HORARIO,
                10,
                CLINICO,
                "Avaliação",
                "Cardiologia"
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaEspecializada(
                1,
                HORARIO,
                10,
                CARDIOLOGISTA,
                "Avaliação",
                "Neurologia"
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaEspecializada(
                1,
                HORARIO,
                10,
                CARDIOLOGISTA,
                "Avaliação",
                " "
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            new ConsultaEspecializada(
                1,
                HORARIO,
                10,
                CARDIOLOGISTA,
                "Avaliação",
                null
            )
        );
        var consulta = new ConsultaEspecializada(
            1,
            HORARIO,
            10,
            new MedicoConsulta(2, " cardiologia "),
            "Avaliação",
            " CARDIOLOGIA "
        );
        assertEquals("CARDIOLOGIA", consulta.getEspecialidade());
        assertEquals(TipoConsulta.ESPECIALIZADA, consulta.getTipo());
        assertThrows(IllegalArgumentException.class, () ->
            consulta.atualizarAgendamento(
                HORARIO.plusDays(1),
                20,
                CLINICO,
                "Retorno"
            )
        );
        assertEquals(HORARIO, consulta.getDataHora());
        assertEquals(10, consulta.getPacienteId());
        assertEquals(2, consulta.getMedico().id());
        consulta.alterarSituacao(SituacaoConsulta.EM_ATENDIMENTO);
        consulta.alterarSituacao(SituacaoConsulta.CONCLUIDA);
        consulta.registrarObservacoes("Avaliação cardiológica registrada");
    }

    @ParameterizedTest
    @EnumSource(PrioridadeAtendimento.class)
    void emergenciaRegistraPrioridadeEIdentificaEsperaInicioEConclusao(
        PrioridadeAtendimento prioridade
    ) {
        var consulta = new AtendimentoEmergencia(
            1,
            HORARIO,
            10,
            CLINICO,
            "Dor",
            prioridade
        );
        assertEquals(TipoConsulta.EMERGENCIA, consulta.getTipo());
        assertEquals(prioridade, consulta.getPrioridade());
        assertTrue(consulta.isAguardandoAvaliacao());
        consulta.alterarSituacao(SituacaoConsulta.EM_ATENDIMENTO);
        assertFalse(consulta.isAguardandoAvaliacao());
        assertEquals(SituacaoConsulta.EM_ATENDIMENTO, consulta.getSituacao());
        consulta.alterarSituacao(SituacaoConsulta.CONCLUIDA);
        assertEquals(SituacaoConsulta.CONCLUIDA, consulta.getSituacao());
        assertFalse(consulta.isAguardandoAvaliacao());
    }

    @Test
    void emergenciaExigePrioridadeENaoConsideraCancelamentoComoEspera() {
        assertThrows(IllegalArgumentException.class, () ->
            new AtendimentoEmergencia(1, HORARIO, 10, CLINICO, "Dor", null)
        );
        var consulta = new AtendimentoEmergencia(
            1,
            HORARIO,
            10,
            CLINICO,
            "Dor",
            PrioridadeAtendimento.ALTA
        );
        consulta.alterarSituacao(SituacaoConsulta.CONFIRMADA);
        assertTrue(consulta.isAguardandoAvaliacao());
        consulta.alterarSituacao(SituacaoConsulta.CANCELADA);
        assertFalse(consulta.isAguardandoAvaliacao());
    }
}
