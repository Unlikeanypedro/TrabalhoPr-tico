package com.example.application;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.model.*;
import com.example.application.repository.*;
import com.example.application.service.ConsultaService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsultaViewTest {
    @LocalServerPort int porta;
    @Autowired ConsultaService service;
    @Autowired ConsultaRepository repository;
    @Autowired ReferenciasConsultaEmMemoria referencias;
    private final HttpClient cliente = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 11, 20, 10, 30);
    private static final String DADOS = "tipo=ROTINA&dataHora=2026-11-20T10%3A30&pacienteId=8010&medicoId=8001&motivo=Avaliacao";

    @BeforeEach void preparar() {
        repository.listar().forEach(c -> repository.excluir(c.getId()));
        referencias.registrarPaciente(8010);
        referencias.registrarPaciente(8020);
        referencias.registrarMedico(new MedicoConsulta(8001, null));
        referencias.registrarMedico(new MedicoConsulta(8002, "Cardiologia"));
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String formulario) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .method(metodo, formulario == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(formulario))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void renderizaTemplateComEstruturaExistenteSemDadosFicticios() throws Exception {
        var pagina = enviar("GET", "/consultas.html", null);
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("Nenhuma consulta encontrada."));
        assertTrue(pagina.body().contains("class=\"cabecalho\""));
        assertTrue(pagina.body().contains("class=\"filtros\""));
        assertTrue(pagina.body().contains("id=\"tabela-consultas\""));
        assertTrue(pagina.body().contains("href=\"/css/style.css\""));
        assertFalse(pagina.body().contains("th:"));
        assertFalse(pagina.body().contains("Profissional 1"));
        assertFalse(pagina.body().contains("id=\"formulario-consulta\""));
        var nova = enviar("GET", "/consultas.html/nova", null);
        assertEquals(200, nova.statusCode());
        assertTrue(nova.body().contains("id=\"formulario-consulta\""));
        assertTrue(nova.body().contains("action=\"/consultas.html\""));
        assertTrue(nova.body().contains("value=\"8002\""));
    }

    @Test void cadastroEEdicaoSalvamMesmoIdEMostramMensagens() throws Exception {
        assertEquals(302, enviar("POST", "/consultas.html", DADOS).statusCode());
        var consulta = service.listar(null, 8001L, null, null).getFirst();
        var pagina = enviar("GET", "/consultas.html", null);
        assertTrue(pagina.body().contains("Consulta salva."));
        assertTrue(pagina.body().contains("20/11/2026 10:30"));
        assertTrue(pagina.body().contains("Médico #8001"));
        var editar = enviar("GET", "/consultas.html/editar/" + consulta.getId(), null);
        assertEquals(200, editar.statusCode());
        assertTrue(editar.body().contains("Editar consulta #" + consulta.getId()));
        assertTrue(editar.body().contains("value=\"Avaliacao\""));
        assertTrue(editar.body().contains("2026-11-20T10:30:00"));
        assertEquals(302, enviar("POST", "/consultas.html", DADOS + "&id=" + consulta.getId()
            + "&especialidade=").statusCode());
        assertEquals(1, service.listar(null, null, null, null).size());
        assertEquals(302, enviar("POST", "/consultas.html", DADOS.replace("8010", "8020")
            .replace("Avaliacao", "Retorno") + "&id=" + consulta.getId()).statusCode());
        assertEquals(8020, service.buscar(consulta.getId()).getPacienteId());
        assertEquals("Retorno", service.buscar(consulta.getId()).getMotivo());
    }

    @Test void cadastraEspecializadaEEmergenciaEValidaCamposEspecificos() throws Exception {
        var especializada = DADOS.replace("ROTINA", "ESPECIALIZADA").replace("8001", "8002") + "&especialidade=Cardiologia";
        assertEquals(302, enviar("POST", "/consultas.html", especializada).statusCode());
        assertEquals(302, enviar("POST", "/consultas.html", DADOS.replace("ROTINA", "EMERGENCIA") + "&prioridade=ALTA").statusCode());
        assertEquals(1, service.listar(null, null, TipoConsulta.ESPECIALIZADA, null).size());
        assertEquals(1, service.listar(null, null, TipoConsulta.EMERGENCIA, null).size());
        var consulta = service.listar(null, null, TipoConsulta.ESPECIALIZADA, null).getFirst();
        var editar = enviar("GET", "/consultas.html/editar/" + consulta.getId(), null);
        assertEquals(200, editar.statusCode());
        assertTrue(editar.body().contains("name=\"tipo\" value=\"ESPECIALIZADA\""));
        assertTrue(editar.body().contains("value=\"Cardiologia\""));
        var invalida = enviar("POST", "/consultas.html", especializada.replace("8002", "8001"));
        assertEquals(200, invalida.statusCode());
        assertTrue(invalida.body().contains("especialidade do médico"));
        var semPrioridade = enviar("POST", "/consultas.html", DADOS.replace("ROTINA", "EMERGENCIA"));
        assertEquals(200, semPrioridade.statusCode());
        assertTrue(semPrioridade.body().contains("Informe a prioridade"));
        assertEquals(2, service.listar(null, null, null, null).size());
    }

    @Test void errosDeBindingEVinculosPreservamFormularioSemGravar() throws Exception {
        for (String dados : new String[] { DADOS.replace("8010", "abc"),
                DADOS.replace("2026-11-20T10%3A30", "invalido"), DADOS.replace("ROTINA", "INVALIDO") }) {
            var resposta = enviar("POST", "/consultas.html", dados);
            assertEquals(200, resposta.statusCode());
            assertTrue(resposta.body().contains("Verifique os IDs"));
            assertTrue(resposta.body().contains("value=\"Avaliacao\""));
        }
        var ausente = enviar("POST", "/consultas.html", DADOS.replace("8010", "999999"));
        assertEquals(200, ausente.statusCode());
        assertTrue(ausente.body().contains("Paciente não encontrado."));
        assertTrue(ausente.body().contains("value=\"999999\""));
        var semMotivo = enviar("POST", "/consultas.html", DADOS.replace("Avaliacao", ""));
        assertEquals(200, semMotivo.statusCode());
        assertTrue(semMotivo.body().contains("Motivo deve ser informado."));
        assertTrue(service.listar(null, null, null, null).isEmpty());
    }

    @Test void filtrosOrdenacaoEPaginacaoFuncionamNoServidor() throws Exception {
        for (int i = 0; i < 7; i++) service.agendarRotina(HORARIO.plusMinutes(i), 8010, 8001, "Consulta " + i);
        var primeira = enviar("GET", "/consultas.html?data=2026-11-20&medicoId=8001&tipo=ROTINA&ordem=data-desc", null);
        assertEquals(200, primeira.statusCode());
        assertTrue(primeira.body().contains("Exibindo 1 a 5 de 7 consultas"));
        assertTrue(primeira.body().indexOf("20/11/2026 10:36") < primeira.body().indexOf("20/11/2026 10:35"));
        assertFalse(primeira.body().contains("20/11/2026 10:30"));
        assertTrue(primeira.body().contains("medicoId=8001"));
        assertTrue(primeira.body().contains("tipo=ROTINA"));
        var segunda = enviar("GET", "/consultas.html?medicoId=8001&ordem=data-desc&pagina=1", null).body();
        assertTrue(segunda.contains("Exibindo 6 a 7 de 7 consultas"));
        assertTrue(segunda.contains("20/11/2026 10:30"));
        assertFalse(segunda.contains("20/11/2026 10:36"));
        assertTrue(enviar("GET", "/consultas.html?pagina=-1", null).body().contains("Exibindo 1 a 5"));
        assertTrue(enviar("GET", "/consultas.html?pagina=999", null).body().contains("Exibindo 6 a 7"));
        assertTrue(enviar("GET", "/consultas.html?tipo=ESPECIALIZADA", null).body().contains("Nenhuma consulta encontrada."));
        for (String ordem : new String[] {"profissional-asc", "profissional-desc", "tipo-asc", "tipo-desc", "invalida"})
            assertEquals(200, enviar("GET", "/consultas.html?ordem=" + ordem, null).statusCode());
    }

    @Test void atendimentoPermiteSomenteTransicoesValidasEObservacoesAposInicio() throws Exception {
        long id = service.agendarRotina(HORARIO, 8010, 8001, "Avaliação").getId();
        String local = "/consultas.html/atendimento/" + id;
        var pagina = enviar("GET", local, null);
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("value=\"CONFIRMADA\""));
        assertFalse(pagina.body().contains("value=\"CONCLUIDA\""));
        assertFalse(pagina.body().contains("name=\"observacoes\""));
        var invalida = enviar("POST", "/consultas.html/" + id + "/situacao", "situacao=CONCLUIDA");
        assertEquals(200, invalida.statusCode());
        assertTrue(invalida.body().contains("Não é permitido alterar"));
        assertEquals(200, enviar("POST", "/consultas.html/" + id + "/situacao", "situacao=INVALIDA").statusCode());
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/situacao", "situacao=EM_ATENDIMENTO").statusCode());
        assertTrue(enviar("GET", local, null).body().contains("name=\"observacoes\""));
        String observacoes = URLEncoder.encode("<script>alert('x')</script>", StandardCharsets.UTF_8);
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/observacoes", "observacoes=" + observacoes).statusCode());
        var observada = enviar("GET", local, null).body();
        assertFalse(observada.contains("<script>alert('x')</script>"));
        assertTrue(observada.contains("&lt;script&gt;"));
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/situacao", "situacao=CONCLUIDA").statusCode());
        var concluida = enviar("GET", local, null).body();
        assertTrue(concluida.contains("Concluída"));
        assertFalse(concluida.contains("name=\"situacao\""));
        assertEquals(409, enviar("GET", "/consultas.html/editar/" + id, null).statusCode());
        var erro = enviar("POST", "/consultas.html", DADOS + "&id=" + id);
        assertEquals(200, erro.statusCode());
        assertTrue(erro.body().contains("Só é possível editar"));
        assertEquals(SituacaoConsulta.CONCLUIDA, service.buscar(id).getSituacao());
    }

    @Test void exclusaoProtegeHistoricoEExibeErro() throws Exception {
        long id = service.agendarRotina(HORARIO, 8010, 8001, "Avaliação").getId();
        service.alterarSituacao(id, SituacaoConsulta.EM_ATENDIMENTO);
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/excluir", "").statusCode());
        assertTrue(enviar("GET", "/consultas.html", null).body().contains("Não é possível excluir"));
        assertNotNull(service.buscar(id));
        long removivel = service.agendarRotina(HORARIO, 8010, 8001, "Avaliação").getId();
        assertEquals(302, enviar("POST", "/consultas.html/" + removivel + "/excluir", "").statusCode());
        assertTrue(enviar("GET", "/consultas.html", null).body().contains("Consulta excluída."));
        assertThrows(NaoEncontradoException.class, () -> service.buscar(removivel));
    }

    @Test void recursosInexistentesEParametrosInvalidosRetornamErrosHttp() throws Exception {
        assertEquals(404, enviar("GET", "/consultas.html/editar/999999", null).statusCode());
        assertEquals(404, enviar("GET", "/consultas.html/atendimento/999999", null).statusCode());
        for (String parametro : new String[] {"data=abc", "medicoId=abc", "tipo=invalido", "situacao=invalida", "pagina=abc", "medicoId=0"})
            assertEquals(400, enviar("GET", "/consultas.html?" + parametro, null).statusCode());
        assertEquals(200, enviar("GET", "/index.html", null).statusCode());
        assertEquals(200, enviar("GET", "/css/style.css", null).statusCode());
    }

    @ParameterizedTest
    @EnumSource(TipoConsulta.class)
    void interfaceEApiCompartilhamCadastroESituacaoDasTresModalidades(TipoConsulta tipo) throws Exception {
        String dados = switch (tipo) {
            case ROTINA -> DADOS;
            case ESPECIALIZADA -> DADOS.replace("ROTINA", "ESPECIALIZADA").replace("8001", "8002")
                + "&especialidade=Cardiologia";
            case EMERGENCIA -> DADOS.replace("ROTINA", "EMERGENCIA") + "&prioridade=CRITICA";
        };
        assertEquals(302, enviar("POST", "/consultas.html", dados).statusCode());
        long id = service.listar(null, null, tipo, null).getFirst().getId();
        var consultaApi = enviar("GET", "/consultas/" + id, null);
        assertEquals(200, consultaApi.statusCode());
        assertTrue(consultaApi.body().contains("\"tipo\":\"" + tipo.name() + "\""));
        assertTrue(consultaApi.body().contains("\"situacao\":\"AGENDADA\""));
        var iniciada = cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta
                + "/consultas/" + id + "/situacao"))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"situacao\":\"EM_ATENDIMENTO\"}"))
            .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, iniciada.statusCode());
        var atendimento = enviar("GET", "/consultas.html/atendimento/" + id, null);
        assertEquals(200, atendimento.statusCode());
        assertTrue(atendimento.body().contains("Em atendimento"));
        assertTrue(atendimento.body().contains("name=\"observacoes\""));
        if (tipo == TipoConsulta.EMERGENCIA) assertTrue(atendimento.body().contains("Crítica"));
        if (tipo == TipoConsulta.ESPECIALIZADA) assertTrue(atendimento.body().contains("Cardiologia"));
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/situacao", "situacao=CONCLUIDA").statusCode());
        assertTrue(enviar("GET", "/consultas/" + id, null).body().contains("\"situacao\":\"CONCLUIDA\""));
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/excluir", "").statusCode());
        assertEquals(200, enviar("GET", "/consultas/" + id, null).statusCode());
    }

    @Test void ausenciaRegistradaNaInterfaceNaoPodeSerReabertaPelaApi() throws Exception {
        long id = service.agendarRotina(HORARIO, 8010, 8001, "Avaliação").getId();
        assertEquals(302, enviar("POST", "/consultas.html/" + id + "/situacao", "situacao=NAO_COMPARECEU").statusCode());
        var atendimento = enviar("GET", "/consultas.html/atendimento/" + id, null);
        assertEquals(200, atendimento.statusCode());
        assertTrue(atendimento.body().contains("Não compareceu"));
        assertFalse(atendimento.body().contains("name=\"situacao\""));
        assertFalse(atendimento.body().contains("name=\"observacoes\""));
        var resposta = cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta
                + "/consultas/" + id + "/situacao"))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"situacao\":\"AGENDADA\"}"))
            .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(409, resposta.statusCode());
        assertEquals(SituacaoConsulta.NAO_COMPARECEU, service.buscar(id).getSituacao());
    }
}
