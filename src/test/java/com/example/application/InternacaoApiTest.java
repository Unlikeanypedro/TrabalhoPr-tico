package com.example.application;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.application.service.QuartoService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternacaoApiTest {
    @LocalServerPort int porta;
    @Autowired QuartoService quartos;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final LocalDate hoje = LocalDate.now();

    private HttpResponse<String> enviar(String metodo, String caminho, String json) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/json")
            .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    private String internacao(long pacienteId, long quartoId, LocalDate entrada, LocalDate prevista) {
        return "{\"pacienteId\":" + pacienteId + ",\"profissionalId\":7,\"quartoId\":" + quartoId
            + ",\"dataEntrada\":\"" + entrada + "\",\"dataPrevistaAlta\":\"" + prevista
            + "\",\"observacoes\":\"Pós-operatório\"}";
    }

    @Test void cicloCompletoDaInternacao() throws Exception {
        long origem = quartos.criar(6001, 6, 1).getId();
        long destino = quartos.criar(6002, 6, 2).getId();

        var criada = enviar("POST", "/internacoes", internacao(910001, origem, hoje.minusDays(2), hoje.plusDays(3)));
        assertEquals(201, criada.statusCode(), criada.body());
        assertTrue(criada.body().contains("\"situacao\":\"ATIVA\""));
        assertTrue(criada.body().contains("\"dataEntrada\":\"" + hoje.minusDays(2) + "\""));
        String local = criada.headers().firstValue("Location").orElseThrow();
        assertEquals(1, quartos.buscar(origem).ocupacaoAtual());
        assertEquals(200, enviar("GET", local, null).statusCode());
        assertTrue(enviar("GET", "/internacoes?pacienteId=910001&situacao=ATIVA", null).body().contains("910001"));

        var lotado = enviar("POST", "/internacoes", internacao(910002, origem, hoje, hoje.plusDays(1)));
        assertEquals(409, lotado.statusCode());
        assertTrue(lotado.body().contains("mensagem"));
        assertEquals(409, enviar("POST", "/internacoes", internacao(910001, destino, hoje, hoje)).statusCode());

        var editada = enviar("PUT", local, "{\"profissionalId\":8,\"dataPrevistaAlta\":\"" + hoje.plusDays(10)
            + "\",\"observacoes\":\"Prorrogada\"}");
        assertEquals(200, editada.statusCode(), editada.body());
        assertTrue(editada.body().contains("\"profissionalId\":8"));

        var transferida = enviar("PATCH", local + "/transferencia", "{\"quartoId\":" + destino + "}");
        assertEquals(200, transferida.statusCode(), transferida.body());
        assertEquals(0, quartos.buscar(origem).ocupacaoAtual());
        assertEquals(1, quartos.buscar(destino).ocupacaoAtual());

        var alta = enviar("PATCH", local + "/alta", "{\"dataEfetivaAlta\":\"" + hoje + "\",\"observacoes\":\"Estável\"}");
        assertEquals(200, alta.statusCode(), alta.body());
        assertTrue(alta.body().contains("\"situacao\":\"FINALIZADA\""));
        assertEquals(0, quartos.buscar(destino).ocupacaoAtual());
        assertEquals(409, enviar("PATCH", local + "/alta", "{\"dataEfetivaAlta\":\"" + hoje + "\"}").statusCode());
        assertEquals(409, enviar("PUT", local, "{\"profissionalId\":8,\"dataPrevistaAlta\":\"" + hoje + "\"}").statusCode());
    }

    @Test void errosDeEntrada() throws Exception {
        long quarto = quartos.criar(6003, 6, 2).getId();
        assertEquals(400, enviar("POST", "/internacoes", "{}").statusCode());
        assertEquals(400, enviar("POST", "/internacoes", "{quebrado").statusCode());
        assertEquals(400, enviar("POST", "/internacoes",
            internacao(910010, quarto, hoje.plusDays(1), hoje.plusDays(2))).statusCode());
        assertEquals(400, enviar("POST", "/internacoes",
            internacao(910010, quarto, hoje, hoje.minusDays(1))).statusCode());
        assertEquals(400, enviar("POST", "/internacoes",
            internacao(910010, quarto, hoje, hoje).replace(hoje.toString(), "09/11/2026")).statusCode());
        assertEquals(404, enviar("POST", "/internacoes", internacao(910010, 999999, hoje, hoje)).statusCode());
        assertEquals(0, quartos.buscar(quarto).ocupacaoAtual());
        assertEquals(400, enviar("GET", "/internacoes?situacao=invalida", null).statusCode());
        assertEquals(400, enviar("GET", "/internacoes/abc", null).statusCode());
        assertEquals(404, enviar("GET", "/internacoes/999999", null).statusCode());
        assertEquals("[]", enviar("GET", "/internacoes?pacienteId=910010", null).body());
    }

    @Test void serveTemplateThymeleaf() throws Exception {
        var tela = enviar("GET", "/internacoes.html", null);
        assertEquals(200, tela.statusCode());
        assertTrue(tela.body().contains("action=\"/internacoes.html\""));
        assertFalse(tela.body().contains("th:"));
    }
}
