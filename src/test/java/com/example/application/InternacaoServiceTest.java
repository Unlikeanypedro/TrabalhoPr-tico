package com.example.application;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.example.application.exception.*;
import com.example.application.model.*;
import com.example.application.repository.InternacaoRepositoryEmMemoria;
import com.example.application.repository.QuartoRepositoryEmMemoria;
import com.example.application.service.InternacaoService;
import com.example.application.service.QuartoService;

class InternacaoServiceTest {
    private final LocalDate hoje = LocalDate.now();
    private QuartoService quartos;
    private InternacaoService service;

    @BeforeEach void preparar() {
        quartos = new QuartoService(new QuartoRepositoryEmMemoria());
        service = new InternacaoService(new InternacaoRepositoryEmMemoria(), quartos);
    }

    private Internacao internar(long pacienteId, long quartoId) {
        return service.registrar(pacienteId, 20L, quartoId, hoje.minusDays(3), hoje.plusDays(2), "Pós-operatório");
    }

    @Test void registrarOcupaVagaDoQuarto() {
        long quarto = quartos.criar(101, 1, 2).getId();
        var internacao = internar(1, quarto);
        assertTrue(internacao.estaAtiva());
        assertEquals(SituacaoInternacao.ATIVA, service.buscar(internacao.getId()).getSituacao());
        assertEquals(1, quartos.buscar(quarto).ocupacaoAtual());
        assertTrue(quartos.buscar(quarto).getInternacoesAtivas().contains(internacao.getId()));
    }

    @Test void quartoLotadoRecusaNovaInternacaoSemGravar() {
        long quarto = quartos.criar(204, 2, 1).getId();
        internar(1, quarto);
        assertEquals(SituacaoQuarto.OCUPADO, quartos.buscar(quarto).getSituacao());
        assertThrows(ConflitoException.class, () -> internar(2, quarto));
        assertEquals(1, service.listar(null, null, null, null).size());
        assertEquals(1, quartos.buscar(quarto).ocupacaoAtual());
    }

    @Test void pacienteNaoPodeTerDuasInternacoesAtivas() {
        long a = quartos.criar(101, 1, 2).getId();
        long b = quartos.criar(102, 1, 2).getId();
        var primeira = internar(1, a);
        assertThrows(ConflitoException.class, () -> internar(1, b));
        service.darAlta(primeira.getId(), hoje, null);
        internar(1, b);
        assertEquals(2, service.listar(1L, null, null, null).size());
    }

    @Test void dadosInvalidosNaoOcupamQuarto() {
        long quarto = quartos.criar(101, 1, 2).getId();
        assertThrows(IllegalArgumentException.class,
            () -> service.registrar(null, 20L, quarto, hoje, hoje, null));
        assertThrows(IllegalArgumentException.class,
            () -> service.registrar(1L, 0L, quarto, hoje, hoje, null));
        assertThrows(IllegalArgumentException.class,
            () -> service.registrar(1L, 20L, quarto, hoje.plusDays(1), hoje.plusDays(5), null));
        assertThrows(IllegalArgumentException.class,
            () -> service.registrar(1L, 20L, quarto, hoje, hoje.minusDays(1), null));
        assertThrows(IllegalArgumentException.class,
            () -> service.registrar(1L, 20L, quarto, null, hoje, null));
        assertThrows(NaoEncontradoException.class,
            () -> service.registrar(1L, 20L, 999L, hoje, hoje, null));
        assertEquals(0, quartos.buscar(quarto).ocupacaoAtual());
        assertTrue(service.listar(null, null, null, null).isEmpty());
    }

    @Test void altaFinalizaELiberaVaga() {
        long quarto = quartos.criar(204, 2, 1).getId();
        var internacao = internar(1, quarto);
        var finalizada = service.darAlta(internacao.getId(), hoje, "Paciente estável");
        assertEquals(SituacaoInternacao.FINALIZADA, finalizada.getSituacao());
        assertEquals(hoje, finalizada.getDataEfetivaAlta());
        assertTrue(finalizada.getObservacoes().contains("Alta: Paciente estável"));
        assertEquals(0, quartos.buscar(quarto).ocupacaoAtual());
        assertEquals(SituacaoQuarto.DISPONIVEL, quartos.buscar(quarto).getSituacao());
    }

    @Test void altaInvalidaNaoAlteraNada() {
        long quarto = quartos.criar(101, 1, 2).getId();
        long id = internar(1, quarto).getId();
        assertThrows(IllegalArgumentException.class, () -> service.darAlta(id, hoje.minusDays(10), null));
        assertThrows(IllegalArgumentException.class, () -> service.darAlta(id, hoje.plusDays(1), null));
        assertThrows(IllegalArgumentException.class, () -> service.darAlta(id, null, null));
        assertTrue(service.buscar(id).estaAtiva());
        assertEquals(1, quartos.buscar(quarto).ocupacaoAtual());
        service.darAlta(id, hoje, null);
        assertThrows(ConflitoException.class, () -> service.darAlta(id, hoje, null));
        assertThrows(NaoEncontradoException.class, () -> service.darAlta(999, hoje, null));
    }

    @Test void atualizarSoEmInternacaoAtiva() {
        long quarto = quartos.criar(101, 1, 2).getId();
        long id = internar(1, quarto).getId();
        var atualizada = service.atualizar(id, 21L, hoje.plusDays(10), "Prorrogada");
        assertEquals(21, atualizada.getProfissionalId());
        assertEquals(hoje.plusDays(10), atualizada.getDataPrevistaAlta());
        assertEquals("Prorrogada", service.buscar(id).getObservacoes());
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(id, 21L, hoje.minusDays(10), null));
        assertEquals(hoje.plusDays(10), service.buscar(id).getDataPrevistaAlta());
        service.darAlta(id, hoje, null);
        assertThrows(ConflitoException.class, () -> service.atualizar(id, 22L, hoje.plusDays(20), null));
    }

    @Test void transferenciaMoveVagaEntreQuartos() {
        long origem = quartos.criar(204, 2, 1).getId();
        long destino = quartos.criar(305, 3, 4).getId();
        long id = internar(1, origem).getId();
        var transferida = service.transferir(id, destino);
        assertEquals(destino, transferida.getQuartoId());
        assertEquals(0, quartos.buscar(origem).ocupacaoAtual());
        assertEquals(1, quartos.buscar(destino).ocupacaoAtual());
        service.darAlta(id, hoje, null);
        assertEquals(0, quartos.buscar(destino).ocupacaoAtual());
    }

    @Test void transferenciaInvalidaMantemQuartoOriginal() {
        long origem = quartos.criar(101, 1, 2).getId();
        long lotado = quartos.criar(204, 2, 1).getId();
        internar(2, lotado);
        long id = internar(1, origem).getId();
        assertThrows(ConflitoException.class, () -> service.transferir(id, lotado));
        assertThrows(ConflitoException.class, () -> service.transferir(id, origem));
        assertThrows(NaoEncontradoException.class, () -> service.transferir(id, 999L));
        assertEquals(origem, service.buscar(id).getQuartoId());
        assertEquals(1, quartos.buscar(origem).ocupacaoAtual());
        assertEquals(1, quartos.buscar(lotado).ocupacaoAtual());
    }

    @Test void editarAlteraDadosETransfereDeUmaVez() {
        long origem = quartos.criar(101, 1, 2).getId();
        long destino = quartos.criar(102, 1, 2).getId();
        long lotado = quartos.criar(103, 1, 1).getId();
        internar(2, lotado);
        long id = internar(1, origem).getId();
        assertThrows(ConflitoException.class,
            () -> service.editar(id, 30L, hoje.plusDays(9), "Não deve gravar", lotado));
        assertEquals(20, service.buscar(id).getProfissionalId());
        service.editar(id, 30L, hoje.plusDays(9), "Transferido", destino);
        var editada = service.buscar(id);
        assertEquals(30, editada.getProfissionalId());
        assertEquals(destino, editada.getQuartoId());
        assertEquals(0, quartos.buscar(origem).ocupacaoAtual());
        assertEquals(1, quartos.buscar(destino).ocupacaoAtual());
    }

    @Test void quartoComInternacaoAtivaNaoPodeSerExcluido() {
        long quarto = quartos.criar(101, 1, 2).getId();
        long id = internar(1, quarto).getId();
        assertThrows(ConflitoException.class, () -> quartos.excluir(quarto));
        service.darAlta(id, hoje, null);
        quartos.excluir(quarto);
    }

    @Test void filtraPorPacienteProfissionalQuartoESituacao() {
        long a = quartos.criar(101, 1, 3).getId();
        long b = quartos.criar(102, 1, 3).getId();
        long primeira = internar(1, a).getId();
        internar(2, a);
        service.registrar(3L, 40L, b, hoje.minusDays(1), hoje.plusDays(1), null);
        service.darAlta(primeira, hoje, null);
        assertEquals(1, service.listar(1L, null, null, null).size());
        assertEquals(1, service.listar(null, 40L, null, null).size());
        assertEquals(2, service.listar(null, null, a, null).size());
        assertEquals(2, service.listar(null, null, null, SituacaoInternacao.ATIVA).size());
        assertEquals(1, service.listar(null, null, a, SituacaoInternacao.FINALIZADA).size());
    }

    @Test void transicaoDeSituacaoSoDeAtivaParaFinalizada() {
        assertTrue(SituacaoInternacao.ATIVA.permiteTransicaoPara(SituacaoInternacao.FINALIZADA));
        assertFalse(SituacaoInternacao.FINALIZADA.permiteTransicaoPara(SituacaoInternacao.ATIVA));
        assertFalse(SituacaoInternacao.ATIVA.permiteTransicaoPara(SituacaoInternacao.ATIVA));
    }
}
