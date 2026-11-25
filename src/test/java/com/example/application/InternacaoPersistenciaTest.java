package com.example.application;

import com.example.application.exception.ConflitoException;
import com.example.application.exception.QuartoSemVagasException;
import com.example.application.model.Internacao;
import com.example.application.model.SituacaoQuarto;
import com.example.application.repository.PacienteRepository;
import com.example.application.repository.ProfissionalRepository;
import com.example.application.service.InternacaoService;
import com.example.application.service.QuartoService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

class InternacaoPersistenciaTest {
    @TempDir Path pasta;
    private final LocalDate hoje = LocalDate.now();

    @Test void ocupacaoEHistoricoContinuamDepoisDeReiniciar() {
        long origem;
        long destino;
        long id;
        try (var app = iniciar("reinicio")) {
            cadastrarVinculos(app);
            var quartos = app.getBean(QuartoService.class);
            var internacoes = app.getBean(InternacaoService.class);
            origem = quartos.criar(701, 7, 1).getId();
            destino = quartos.criar(702, 7, 1).getId();
            id = internar(internacoes, 1, origem).getId();
            assertThrows(QuartoSemVagasException.class, () -> internar(internacoes, 2, origem));
            internacoes.transferir(id, destino);
            quartos.excluir(origem);
        }
        try (var app = iniciar("reinicio")) {
            var quartos = app.getBean(QuartoService.class);
            var internacoes = app.getBean(InternacaoService.class);
            assertEquals(SituacaoQuarto.OCUPADO, quartos.buscar(destino).getSituacao());
            var ativa = internacoes.buscar(id);
            assertEquals(origem, ativa.getHistoricoQuartos().get(0).getQuartoId());
            assertEquals(701, ativa.getHistoricoQuartos().get(0).getNumeroQuarto());
            assertEquals(hoje, ativa.getHistoricoQuartos().get(0).getDataSaida());
            assertEquals(destino, ativa.getHistoricoQuartos().get(1).getQuartoId());
            assertNull(ativa.getHistoricoQuartos().get(1).getDataSaida());
            internacoes.darAlta(id, hoje, "Estável");
        }
        try (var app = iniciar("reinicio")) {
            var quartos = app.getBean(QuartoService.class);
            var alta = app.getBean(InternacaoService.class).buscar(id);
            assertEquals(SituacaoQuarto.DISPONIVEL, quartos.buscar(destino).getSituacao());
            assertFalse(alta.estaAtiva());
            assertEquals(hoje, alta.getDataEfetivaAlta());
            assertEquals(hoje, alta.getHistoricoQuartos().get(1).getDataSaida());
        }
    }

    @Test void falhaAoGravarInternacaoReverteAsVagasNosTresFluxos() {
        try (var app = iniciar("rollback")) {
            cadastrarVinculos(app);
            var quartos = app.getBean(QuartoService.class);
            var internacoes = app.getBean(InternacaoService.class);
            var jdbc = app.getBean(JdbcTemplate.class);
            jdbc.execute("""
                ALTER TABLE sprint3_internacao ADD CONSTRAINT teste_falha_gravacao
                CHECK (observacoes NOT LIKE '%bloqueio-db%')
                """);
            long origem = quartos.criar(703, 7, 1).getId();
            long destino = quartos.criar(704, 7, 1).getId();
            assertThrows(DataIntegrityViolationException.class,
                () -> internacoes.registrar(1L, 10L, origem, hoje, hoje, "bloqueio-db"));
            assertEquals(0, quartos.buscar(origem).ocupacaoAtual());
            assertTrue(internacoes.listar(null, null, null, null).isEmpty());

            long id = internar(internacoes, 1, origem).getId();
            assertThrows(DataIntegrityViolationException.class,
                () -> internacoes.editar(id, 10L, hoje, "bloqueio-db", destino));
            assertEquals(1, quartos.buscar(origem).ocupacaoAtual());
            assertEquals(0, quartos.buscar(destino).ocupacaoAtual());
            assertEquals(origem, internacoes.buscar(id).getQuartoId());
            assertEquals(1, internacoes.buscar(id).getHistoricoQuartos().size());

            assertThrows(DataIntegrityViolationException.class,
                () -> internacoes.darAlta(id, hoje, "bloqueio-db"));
            assertTrue(internacoes.buscar(id).estaAtiva());
            assertEquals(1, quartos.buscar(origem).ocupacaoAtual());
        }
    }

    @Test void bancoSerializaUltimaVagaEInternacaoDoMesmoPaciente() throws Exception {
        try (var app = iniciar("concorrencia")) {
            cadastrarVinculos(app);
            var quartos = app.getBean(QuartoService.class);
            var internacoes = app.getBean(InternacaoService.class);
            long a = quartos.criar(705, 7, 1).getId();
            long b = quartos.criar(706, 7, 2).getId();
            long c = quartos.criar(707, 7, 2).getId();

            assertEquals(1, disputar(() -> internar(internacoes, 1, a), () -> internar(internacoes, 2, a)));
            assertEquals(1, quartos.buscar(a).ocupacaoAtual());
            assertEquals(1, disputar(() -> internar(internacoes, 3, b), () -> internar(internacoes, 3, c)));
            assertEquals(1, quartos.buscar(b).ocupacaoAtual() + quartos.buscar(c).ocupacaoAtual());
            assertEquals(1, internacoes.listar(3L, null, null, null).size());
        }
    }

    private ConfigurableApplicationContext iniciar(String nome) {
        return new SpringApplicationBuilder(DemoApplication.class).web(WebApplicationType.NONE).run(
            "--spring.datasource.url=jdbc:h2:file:" + pasta.resolve(nome) + ";MODE=PostgreSQL",
            "--spring.main.banner-mode=off");
    }

    private void cadastrarVinculos(ConfigurableApplicationContext app) {
        for (long id : new long[]{1, 2, 3}) {
            CadastrosInternacaoTeste.paciente(app.getBean(PacienteRepository.class), id);
        }
        CadastrosInternacaoTeste.profissional(app.getBean(ProfissionalRepository.class), 10);
    }

    private Internacao internar(InternacaoService service, long pacienteId, long quartoId) {
        return service.registrar(pacienteId, 10L, quartoId, hoje.minusDays(1), hoje, null);
    }

    private long disputar(Supplier<Internacao> primeira, Supplier<Internacao> segunda) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> tentar(inicio, primeira));
            var b = executor.submit(() -> tentar(inicio, segunda));
            inicio.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)).stream()
                .filter(sucesso -> sucesso).count();
        }
    }

    private boolean tentar(CountDownLatch inicio, Supplier<Internacao> operacao) throws InterruptedException {
        inicio.await();
        try {
            operacao.get();
            return true;
        } catch (ConflitoException e) {
            return false;
        }
    }
}
