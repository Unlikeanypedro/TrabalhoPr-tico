package com.example.application;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.example.application.exception.*;
import com.example.application.model.*;
import com.example.application.repository.*;
import com.example.application.service.ConsultaService;

class ConsultaServiceTest {
    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 11, 20, 10, 30);
    private ReferenciasConsultaEmMemoria referencias;
    private ConsultaService service;

    @BeforeEach void preparar() {
        referencias = new ReferenciasConsultaEmMemoria();
        referencias.registrarPaciente(10);
        referencias.registrarPaciente(20);
        referencias.registrarMedico(new MedicoConsulta(1, null));
        referencias.registrarMedico(new MedicoConsulta(2, "Cardiologia"));
        service = new ConsultaService(new ConsultaRepositoryEmMemoria(), referencias);
    }

    @Test void agendaAsTresModalidadesComIdentificadoresUnicos() {
        var rotina = service.agendarRotina(HORARIO, 10, 1, "Avaliação geral");
        var especializada = service.agendarEspecializada(HORARIO.plusHours(1), 20, 2, "Retorno", "Cardiologia");
        var emergencia = service.registrarEmergencia(HORARIO.plusHours(2), 10, 1, "Dor", PrioridadeAtendimento.ALTA);
        assertEquals(3, Set.of(rotina.getId(), especializada.getId(), emergencia.getId()).size());
        assertInstanceOf(ConsultaRotina.class, service.buscar(rotina.getId()));
        assertEquals("Cardiologia", ((ConsultaEspecializada) service.buscar(especializada.getId())).getEspecialidade());
        assertEquals(PrioridadeAtendimento.ALTA,
            ((AtendimentoEmergencia) service.buscar(emergencia.getId())).getPrioridade());
    }

    @Test void rejeitaVinculosDesconhecidosOuInvalidosSemSalvar() {
        assertThrows(NaoEncontradoException.class, () -> service.agendarRotina(HORARIO, 99, 1, "Avaliação"));
        assertThrows(NaoEncontradoException.class, () -> service.agendarRotina(HORARIO, 10, 99, "Avaliação"));
        assertThrows(IllegalArgumentException.class, () -> service.agendarRotina(HORARIO, 0, 1, "Avaliação"));
        assertThrows(IllegalArgumentException.class, () -> service.agendarRotina(HORARIO, 10, -1, "Avaliação"));
        assertThrows(NaoEncontradoException.class, () -> service.agendarEspecializada(HORARIO, 99, 2, "Retorno", "Cardiologia"));
        assertThrows(NaoEncontradoException.class, () -> service.registrarEmergencia(HORARIO, 10, 99, "Dor", PrioridadeAtendimento.CRITICA));
        assertTrue(service.listar(null, null, null, null).isEmpty());
    }

    @Test void cadastroLocalComecaVazioENaoInventaVinculos() {
        var cadastro = new ReferenciasConsultaEmMemoria();
        var isolado = new ConsultaService(new ConsultaRepositoryEmMemoria(), cadastro);
        assertFalse(cadastro.pacienteExiste(10));
        assertTrue(cadastro.buscarMedico(1).isEmpty());
        assertThrows(NaoEncontradoException.class, () -> isolado.agendarRotina(HORARIO, 10, 1, "Avaliação"));
        assertThrows(IllegalArgumentException.class, () -> cadastro.registrarPaciente(0));
        assertThrows(IllegalArgumentException.class, () -> cadastro.registrarMedico(null));
    }

    @Test void validaEspecialidadeAPartirDoCadastroENaoSalvaDadosInvalidos() {
        assertThrows(IllegalArgumentException.class,
            () -> service.agendarEspecializada(HORARIO, 10, 1, "Retorno", "Cardiologia"));
        assertThrows(IllegalArgumentException.class,
            () -> service.agendarEspecializada(HORARIO, 10, 2, "Retorno", "Neurologia"));
        assertThrows(IllegalArgumentException.class,
            () -> service.agendarRotina(null, 10, 1, "Avaliação"));
        assertThrows(IllegalArgumentException.class,
            () -> service.registrarEmergencia(HORARIO, 10, 1, "Dor", null));
        assertTrue(service.listar(null, null, null, null).isEmpty());
    }

    @Test void filtraPorDataMedicoTipoESituacaoEOrdenaPorHorario() {
        var tardia = service.agendarRotina(HORARIO.plusDays(1), 10, 1, "Retorno");
        var especializada = service.agendarEspecializada(HORARIO, 20, 2, "Retorno", "Cardiologia");
        var emergencia = service.registrarEmergencia(HORARIO.plusHours(1), 10, 1, "Dor", PrioridadeAtendimento.BAIXA);
        service.alterarSituacao(especializada.getId(), SituacaoConsulta.CONFIRMADA);
        assertEquals(java.util.List.of(especializada.getId(), emergencia.getId(), tardia.getId()),
            service.listar(null, null, null, null).stream().map(Consulta::getId).toList());
        assertEquals(2, service.listar(HORARIO.toLocalDate(), null, null, null).size());
        assertEquals(2, service.listar(null, 1L, null, null).size());
        assertEquals(1, service.listar(null, null, TipoConsulta.EMERGENCIA, null).size());
        var filtradas = service.listar(HORARIO.toLocalDate(), 2L, TipoConsulta.ESPECIALIZADA, SituacaoConsulta.CONFIRMADA);
        assertEquals(1, filtradas.size());
        assertEquals(especializada.getId(), filtradas.getFirst().getId());
        assertTrue(service.listar(HORARIO.toLocalDate(), 1L, TipoConsulta.ESPECIALIZADA, null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.listar(null, 0L, null, null));
    }

    @Test void atualizaAgendamentoSemPermitirQueFalhasAlteremDadosSalvos() {
        long id = service.agendarEspecializada(HORARIO, 10, 2, "Avaliação", "Cardiologia").getId();
        assertThrows(IllegalArgumentException.class,
            () -> service.atualizar(id, HORARIO.plusDays(1), 20, 1, "Retorno"));
        assertThrows(NaoEncontradoException.class,
            () -> service.atualizar(id, HORARIO.plusDays(1), 99, 2, "Retorno"));
        assertEquals(HORARIO, service.buscar(id).getDataHora());
        assertEquals(10, service.buscar(id).getPacienteId());
        service.atualizar(id, HORARIO.plusDays(1), 20, 2, "Retorno");
        assertEquals(HORARIO.plusDays(1), service.buscar(id).getDataHora());
        assertEquals(20, service.buscar(id).getPacienteId());
        assertEquals("Retorno", service.buscar(id).getMotivo());
    }

    @Test void revalidaEspecialidadeAtualAntesDeIniciarConsulta() {
        long id = service.agendarEspecializada(HORARIO, 10, 2, "Retorno", "Cardiologia").getId();
        referencias.registrarMedico(new MedicoConsulta(2, "Neurologia"));
        assertThrows(IllegalArgumentException.class,
            () -> service.alterarSituacao(id, SituacaoConsulta.EM_ATENDIMENTO));
        assertEquals(SituacaoConsulta.AGENDADA, service.buscar(id).getSituacao());
        assertEquals("Cardiologia", service.buscar(id).getMedico().especialidade());
        referencias.registrarMedico(new MedicoConsulta(2, "Cardiologia"));
        service.alterarSituacao(id, SituacaoConsulta.EM_ATENDIMENTO);
        assertEquals(SituacaoConsulta.EM_ATENDIMENTO, service.buscar(id).getSituacao());
    }

    @Test void preservaSituacaoEObservacoesAposFalhasEExigeNovoAgendamentoAposCancelamento() {
        long id = service.agendarRotina(HORARIO, 10, 1, "Avaliação").getId();
        assertThrows(ConflitoException.class, () -> service.alterarSituacao(id, SituacaoConsulta.CONCLUIDA));
        assertThrows(ConflitoException.class, () -> service.registrarObservacoes(id, "Antes"));
        assertThrows(IllegalArgumentException.class, () -> service.alterarSituacao(id, null));
        assertEquals(SituacaoConsulta.AGENDADA, service.buscar(id).getSituacao());
        service.alterarSituacao(id, SituacaoConsulta.EM_ATENDIMENTO);
        service.registrarObservacoes(id, "Avaliação realizada");
        service.alterarSituacao(id, SituacaoConsulta.CONCLUIDA);
        assertThrows(IllegalArgumentException.class, () -> service.registrarObservacoes(id, " "));
        assertEquals("Avaliação realizada", service.buscar(id).getObservacoes());
        service.registrarObservacoes(id, "Orientações finais");
        assertEquals("Orientações finais", service.buscar(id).getObservacoes());
        long cancelada = service.agendarRotina(HORARIO, 10, 1, "Avaliação").getId();
        service.alterarSituacao(cancelada, SituacaoConsulta.CANCELADA);
        assertThrows(ConflitoException.class,
            () -> service.alterarSituacao(cancelada, SituacaoConsulta.CONCLUIDA));
        long nova = service.agendarRotina(HORARIO.plusDays(1), 10, 1, "Novo agendamento").getId();
        assertNotEquals(cancelada, nova);
        assertEquals(SituacaoConsulta.AGENDADA, service.buscar(nova).getSituacao());
    }

    @Test void excluirPreservaConsultasEmAtendimentoEConcluidas() {
        long id = service.agendarRotina(HORARIO, 10, 1, "Avaliação").getId();
        service.alterarSituacao(id, SituacaoConsulta.EM_ATENDIMENTO);
        assertThrows(ConflitoException.class, () -> service.excluir(id));
        service.alterarSituacao(id, SituacaoConsulta.CONCLUIDA);
        assertThrows(ConflitoException.class, () -> service.excluir(id));
        assertNotNull(service.buscar(id));
        long removivel = service.agendarRotina(HORARIO, 20, 1, "Avaliação").getId();
        service.excluir(removivel);
        assertThrows(NaoEncontradoException.class, () -> service.buscar(removivel));
    }

    @Test void objetosDeRetornoNaoPermitemAlterarRepositorioDiretamente() {
        var criada = service.agendarRotina(HORARIO, 10, 1, "Avaliação");
        criada.alterarSituacao(SituacaoConsulta.CANCELADA);
        service.buscar(criada.getId()).alterarSituacao(SituacaoConsulta.CANCELADA);
        service.listar(null, null, null, null).getFirst().alterarSituacao(SituacaoConsulta.CANCELADA);
        assertEquals(SituacaoConsulta.AGENDADA, service.buscar(criada.getId()).getSituacao());
    }

    @Test void operacoesRejeitamConsultaInexistente() {
        assertThrows(NaoEncontradoException.class, () -> service.buscar(99));
        assertThrows(NaoEncontradoException.class, () -> service.atualizar(99, HORARIO, 10, 1, "Avaliação"));
        assertThrows(NaoEncontradoException.class, () -> service.alterarSituacao(99, SituacaoConsulta.CONFIRMADA));
        assertThrows(NaoEncontradoException.class, () -> service.registrarObservacoes(99, "Observação"));
        assertThrows(NaoEncontradoException.class, () -> service.excluir(99));
        assertThrows(IllegalArgumentException.class, () -> service.buscar(0));
    }

    @Test void doisIniciosSimultaneosNaoSobrescrevemTransicao() throws Exception {
        long id = service.agendarRotina(HORARIO, 10, 1, "Avaliação").getId();
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> tarefa = () -> {
                inicio.await();
                try {
                    service.alterarSituacao(id, SituacaoConsulta.EM_ATENDIMENTO);
                    return true;
                } catch (ConflitoException e) { return false; }
            };
            var a = executor.submit(tarefa);
            var b = executor.submit(tarefa);
            inicio.countDown();
            assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
            assertEquals(SituacaoConsulta.EM_ATENDIMENTO, service.buscar(id).getSituacao());
        }
    }
}
