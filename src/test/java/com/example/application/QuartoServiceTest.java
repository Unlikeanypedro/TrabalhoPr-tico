package com.example.application;

import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.example.application.model.*;
import com.example.application.repository.QuartoRepositoryEmMemoria;
import com.example.application.service.QuartoService;
import com.example.application.exception.*;

class QuartoServiceTest {
    private QuartoService service;
    @BeforeEach void preparar() { service = new QuartoService(new QuartoRepositoryEmMemoria()); }

    @Test void cadastroValidaDadosENumeroUnico() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(0, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> service.criar(101, 1, 0));
        var q = service.criar(101, 0, 2);
        assertEquals(0, q.ocupacaoAtual());
        assertEquals(SituacaoQuarto.DISPONIVEL, q.getSituacao());
        assertThrows(ConflitoException.class, () -> service.criar(101, 2, 3));
    }

    @Test void lotacaoEAltaPreservamOutrasInternacoes() {
        long id = service.criar(101, 1, 2).getId();
        service.registrarEntrada(id, 10);
        assertTrue(service.buscar(id).estaDisponivel());
        service.registrarEntrada(id, 11);
        assertEquals(SituacaoQuarto.OCUPADO, service.buscar(id).getSituacao());
        assertThrows(ConflitoException.class, () -> service.registrarEntrada(id, 12));
        assertThrows(ConflitoException.class, () -> service.registrarEntrada(id, 10));
        assertThrows(ConflitoException.class, () -> service.registrarAlta(id, 99));
        service.registrarAlta(id, 10);
        assertEquals(java.util.Set.of(11L), service.buscar(id).getInternacoesAtivas());
        assertTrue(service.buscar(id).estaDisponivel());
        assertThrows(ConflitoException.class, () -> service.registrarAlta(id, 10));
    }

    @Test void alteracoesInvalidasNaoModificamDadosSalvos() {
        long id = service.criar(101, 1, 2).getId();
        service.criar(102, 1, 1);
        service.registrarEntrada(id, 10);
        service.registrarEntrada(id, 11);
        assertThrows(ConflitoException.class, () -> service.atualizar(id, 999, 3, 1));
        assertEquals(101, service.buscar(id).getNumero());
        assertThrows(ConflitoException.class, () -> service.atualizar(id, 102, 1, 2));
        assertThrows(ConflitoException.class, () -> service.excluir(id));
        service.buscar(id).liberarQuarto(10);
        assertEquals(2, service.buscar(id).ocupacaoAtual());
        service.registrarAlta(id, 10);
        service.registrarAlta(id, 11);
        service.excluir(id);
        assertThrows(NaoEncontradoException.class, () -> service.buscar(id));
    }

    @Test void mesmaInternacaoNaoOcupaDoisQuartos() {
        long a = service.criar(101, 1, 2).getId();
        long b = service.criar(102, 1, 2).getId();
        service.registrarEntrada(a, 1);
        assertThrows(ConflitoException.class, () -> service.registrarEntrada(b, 1));
        service.registrarAlta(a, 1);
        service.registrarEntrada(b, 1);
        assertEquals(1, service.buscar(b).ocupacaoAtual());
    }

    @Test void filtraPorNumeroAndarESituacao() {
        long id = service.criar(101, 1, 1).getId();
        service.criar(201, 2, 2);
        service.registrarEntrada(id, 1);
        assertEquals(1, service.listar(101, 1, SituacaoQuarto.OCUPADO).size());
        assertTrue(service.listar(101, 1, SituacaoQuarto.DISPONIVEL).isEmpty());
    }

    @Test void duasEntradasSimultaneasNaoUltrapassamCapacidade() throws Exception {
        long id = service.criar(101, 1, 1).getId();
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarefas = new java.util.ArrayList<Future<Boolean>>();
            for (long internacao = 1; internacao <= 2; internacao++) {
                final long referencia = internacao;
                tarefas.add(executor.submit(() -> {
                    inicio.await();
                    try { service.registrarEntrada(id, referencia); return true; }
                    catch (ConflitoException e) { return false; }
                }));
            }
            inicio.countDown();
            int sucessos = 0;
            for (var tarefa : tarefas) if (tarefa.get(5, TimeUnit.SECONDS)) sucessos++;
            assertEquals(1, sucessos);
            assertEquals(1, service.buscar(id).ocupacaoAtual());
        }
    }
}
