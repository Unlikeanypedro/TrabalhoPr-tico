package com.example.application;

import static org.junit.jupiter.api.Assertions.*;

import com.example.application.model.MedicoConsulta;
import com.example.application.repository.ConsultaRepository;
import com.example.application.repository.ReferenciasConsultaEmMemoria;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsultaApiTest {

    private static final String ROTINA = """
    {"tipo":"ROTINA","dataHora":"2026-11-20T10:30:00",
     "pacienteId":910,"medicoId":901,"motivo":"Avaliação geral"}
    """;
    private static final String ESPECIALIZADA = """
    {"tipo":"ESPECIALIZADA","dataHora":"2026-11-20T11:30:00",
     "pacienteId":920,"medicoId":902,"motivo":"Retorno","especialidade":"Cardiologia"}
    """;
    private static final String EMERGENCIA = """
    {"tipo":"EMERGENCIA","dataHora":"2026-11-20T12:30:00",
     "pacienteId":910,"medicoId":901,"motivo":"Dor","prioridade":"CRITICA"}
    """;

    @LocalServerPort
    int porta;

    @Autowired
    ConsultaRepository repository;

    @Autowired
    ReferenciasConsultaEmMemoria referencias;

    @Autowired
    ObjectMapper mapper;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @BeforeEach
    void preparar() {
        repository.listar().forEach(c -> repository.excluir(c.getId()));
        referencias.registrarPaciente(910);
        referencias.registrarPaciente(920);
        referencias.registrarMedico(new MedicoConsulta(901, null));
        referencias.registrarMedico(new MedicoConsulta(902, "Cardiologia"));
    }

    private HttpResponse<String> enviar(
        String metodo,
        String caminho,
        String json
    ) throws Exception {
        return cliente.send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:" + porta + caminho)
            )
                .header("Content-Type", "application/json")
                .method(
                    metodo,
                    json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json)
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private String criar(String dados) throws Exception {
        var resposta = enviar("POST", "/consultas", dados);
        assertEquals(201, resposta.statusCode(), resposta.body());
        return resposta.headers().firstValue("Location").orElseThrow();
    }

    private void verificarErro(int status, HttpResponse<String> resposta) {
        assertEquals(status, resposta.statusCode(), resposta.body());
        assertTrue(
            mapper.readTree(resposta.body()).hasNonNull("mensagem"),
            resposta.body()
        );
    }

    @Test
    void cicloCrudRetornaDadosELocationEExcluiAgendamento() throws Exception {
        String local = criar(ROTINA);
        assertTrue(local.matches("/consultas/[0-9]+"));
        var resposta = enviar("GET", local, null);
        assertEquals(200, resposta.statusCode());
        var dados = mapper.readTree(resposta.body());
        assertEquals("ROTINA", dados.get("tipo").asText());
        assertEquals("AGENDADA", dados.get("situacao").asText());
        assertEquals(910, dados.get("pacienteId").asLong());
        assertEquals(901, dados.get("medicoId").asLong());
        assertEquals("Avaliação geral", dados.get("motivo").asText());
        assertEquals("", dados.get("observacoes").asText());
        assertTrue(dados.get("especialidade").isNull());
        assertTrue(dados.get("prioridade").isNull());
        String atualizacao = """
        {"dataHora":"2026-11-21T14:00:00","pacienteId":920,"medicoId":902,"motivo":"Retorno"}
        """;
        var editada = enviar("PUT", local, atualizacao);
        assertEquals(200, editada.statusCode());
        assertEquals(
            920,
            mapper.readTree(editada.body()).get("pacienteId").asLong()
        );
        assertEquals(
            "Retorno",
            mapper
                .readTree(enviar("GET", local, null).body())
                .get("motivo")
                .asText()
        );
        assertEquals(204, enviar("DELETE", local, null).statusCode());
        verificarErro(404, enviar("GET", local, null));
        verificarErro(404, enviar("PUT", local, atualizacao));
        verificarErro(
            404,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"CONFIRMADA\"}"
            )
        );
        verificarErro(
            404,
            enviar(
                "PATCH",
                local + "/observacoes",
                "{\"observacoes\":\"Avaliação\"}"
            )
        );
        verificarErro(404, enviar("DELETE", local, null));
    }

    @Test
    void listaModalidadesComFiltrosCombinadosEOrdemCronologica()
        throws Exception {
        criar(EMERGENCIA);
        String local = criar(ESPECIALIZADA);
        criar(ROTINA);
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"CONFIRMADA\"}"
            ).statusCode()
        );
        var lista = mapper.readTree(enviar("GET", "/consultas", null).body());
        assertEquals(3, lista.size());
        assertEquals("ROTINA", lista.get(0).get("tipo").asText());
        assertEquals("ESPECIALIZADA", lista.get(1).get("tipo").asText());
        assertEquals("CRITICA", lista.get(2).get("prioridade").asText());
        var filtrada = enviar(
            "GET",
            "/consultas?data=2026-11-20&medicoId=902&tipo=ESPECIALIZADA&situacao=CONFIRMADA",
            null
        );
        assertEquals(200, filtrada.statusCode());
        assertEquals(1, mapper.readTree(filtrada.body()).size());
        assertEquals(
            "Cardiologia",
            mapper
                .readTree(filtrada.body())
                .get(0)
                .get("especialidade")
                .asText()
        );
        assertEquals(
            2,
            mapper
                .readTree(enviar("GET", "/consultas?medicoId=901", null).body())
                .size()
        );
        assertEquals(
            "[]",
            enviar("GET", "/consultas?data=2026-11-21", null).body()
        );
        assertEquals(
            "[]",
            enviar(
                "GET",
                "/consultas?tipo=EMERGENCIA&situacao=CONCLUIDA",
                null
            ).body()
        );
    }

    @Test
    void transicoesInvalidasRetornamConflitoEPreservamConsulta()
        throws Exception {
        String local = criar(ROTINA);
        verificarErro(
            409,
            enviar("PATCH", local + "/situacao", "{\"situacao\":\"CONCLUIDA\"}")
        );
        verificarErro(
            409,
            enviar(
                "PATCH",
                local + "/observacoes",
                "{\"observacoes\":\"Antes\"}"
            )
        );
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"CONFIRMADA\"}"
            ).statusCode()
        );
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"EM_ATENDIMENTO\"}"
            ).statusCode()
        );
        verificarErro(409, enviar("DELETE", local, null));
        verificarErro(409, enviar("PUT", local, ROTINA));
        verificarErro(
            409,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"NAO_COMPARECEU\"}"
            )
        );
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/observacoes",
                "{\"observacoes\":\" Avaliação realizada \"}"
            ).statusCode()
        );
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"CONCLUIDA\"}"
            ).statusCode()
        );
        verificarErro(
            409,
            enviar("PATCH", local + "/situacao", "{\"situacao\":\"AGENDADA\"}")
        );
        verificarErro(409, enviar("DELETE", local, null));
        verificarErro(
            400,
            enviar("PATCH", local + "/observacoes", "{\"observacoes\":\" \"}")
        );
        var salva = mapper.readTree(enviar("GET", local, null).body());
        assertEquals("CONCLUIDA", salva.get("situacao").asText());
        assertEquals("Avaliação realizada", salva.get("observacoes").asText());
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/observacoes",
                "{\"observacoes\":\"Orientações finais\"}"
            ).statusCode()
        );
    }

    @Test
    void cancelamentoExigeNovoAgendamentoParaRealizarConsulta()
        throws Exception {
        String local = criar(ROTINA);
        assertEquals(
            200,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"CANCELADA\"}"
            ).statusCode()
        );
        verificarErro(
            409,
            enviar("PATCH", local + "/situacao", "{\"situacao\":\"CONCLUIDA\"}")
        );
        verificarErro(
            409,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"EM_ATENDIMENTO\"}"
            )
        );
        String nova = criar(ROTINA);
        assertNotEquals(local, nova);
        assertEquals(
            "AGENDADA",
            mapper
                .readTree(enviar("GET", nova, null).body())
                .get("situacao")
                .asText()
        );
    }

    @Test
    void rejeitaCorposInvalidosSemCriarConsultas() throws Exception {
        String[] invalidos = {
            "{}",
            "null",
            "{quebrado",
            "[]",
            "",
            ROTINA.replace("ROTINA", "INVALIDA"),
            ROTINA.replace("\"ROTINA\"", "null"),
            ROTINA.replace("910", "null"),
            ROTINA.replace("901", "null"),
            ROTINA.replace("910", "0"),
            ROTINA.replace("901", "-1"),
            ROTINA.replace("910", "910.5"),
            ROTINA.replace("901", "901.5"),
            ROTINA.replace("2026-11-20T10:30:00", "2026-02-30T10:30:00"),
            ROTINA.replace("\"2026-11-20T10:30:00\"", "null"),
            ROTINA.replace("Avaliação geral", " "),
            ESPECIALIZADA.replace("Cardiologia", " "),
            ESPECIALIZADA.replace("\"Cardiologia\"", "null"),
            EMERGENCIA.replace("\"CRITICA\"", "null"),
            EMERGENCIA.replace("CRITICA", "URGENTE"),
            EMERGENCIA.replace("EMERGENCIA", "ROTINA"),
            ESPECIALIZADA.replace("ESPECIALIZADA", "ROTINA"),
        };
        for (String dados : invalidos)
            verificarErro(400, enviar("POST", "/consultas", dados));
        assertEquals("[]", enviar("GET", "/consultas", null).body());
    }

    @Test
    void rejeitaVinculosAusentesEEspecialidadeIncompativel() throws Exception {
        verificarErro(
            404,
            enviar("POST", "/consultas", ROTINA.replace("910", "999910"))
        );
        verificarErro(
            404,
            enviar("POST", "/consultas", ROTINA.replace("901", "999901"))
        );
        verificarErro(
            400,
            enviar("POST", "/consultas", ESPECIALIZADA.replace("902", "901"))
        );
        verificarErro(
            400,
            enviar(
                "POST",
                "/consultas",
                ESPECIALIZADA.replace("Cardiologia", "Neurologia")
            )
        );
        String local = criar(ESPECIALIZADA);
        var antes = enviar("GET", local, null).body();
        verificarErro(
            400,
            enviar("PUT", local, ESPECIALIZADA.replace("902", "901"))
        );
        verificarErro(
            404,
            enviar("PUT", local, ESPECIALIZADA.replace("920", "999920"))
        );
        assertEquals(antes, enviar("GET", local, null).body());
        referencias.registrarMedico(new MedicoConsulta(902, "Neurologia"));
        verificarErro(
            400,
            enviar(
                "PATCH",
                local + "/situacao",
                "{\"situacao\":\"EM_ATENDIMENTO\"}"
            )
        );
        assertEquals(antes, enviar("GET", local, null).body());
    }

    @Test
    void validaParametrosECorposDeEdicaoSemAlterarRegistro() throws Exception {
        String local = criar(ROTINA);
        var antes = enviar("GET", local, null).body();
        for (String caminho : new String[] {
            "/consultas/abc",
            "/consultas/0",
            "/consultas?data=ontem",
            "/consultas?data=2026-02-30",
            "/consultas?medicoId=abc",
            "/consultas?medicoId=0",
            "/consultas?tipo=INVALIDO",
            "/consultas?situacao=INVALIDA",
        })
            verificarErro(400, enviar("GET", caminho, null));
        for (String dados : new String[] {
            "{}",
            "null",
            "{quebrado",
            ROTINA.replace("910", "910.5"),
        })
            verificarErro(400, enviar("PUT", local, dados));
        for (String dados : new String[] {
            "{}",
            "null",
            "{\"situacao\":null}",
            "{\"situacao\":\"INVALIDA\"}",
        })
            verificarErro(400, enviar("PATCH", local + "/situacao", dados));
        for (String dados : new String[] {
            "{}",
            "null",
            "{\"observacoes\":null}",
            "{\"observacoes\":\" \"}",
        })
            verificarErro(400, enviar("PATCH", local + "/observacoes", dados));
        assertEquals(antes, enviar("GET", local, null).body());
    }
}
