package com.example.application;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.application.service.QuartoService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuartoApiTest {
    @LocalServerPort int porta;
    @Autowired QuartoService service;
    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> enviar(String metodo, String caminho, String json) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/json")
            .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void cicloCrudErrosEFiltros() throws Exception {
        String dados = "{\"numero\":501,\"andar\":5,\"capacidadeMaxima\":2}";
        var criado = enviar("POST", "/quartos", dados);
        assertEquals(201, criado.statusCode());
        assertTrue(criado.body().contains("\"numero\":501"));
        assertTrue(criado.body().contains("\"capacidadeMaxima\":2"));
        assertTrue(criado.body().contains("\"ocupacaoAtual\":0"));
        assertTrue(criado.body().contains("\"situacao\":\"DISPONIVEL\""));
        String local = criado.headers().firstValue("Location").orElseThrow();
        long id = Long.parseLong(local.substring(local.lastIndexOf('/') + 1));
        assertEquals(200, enviar("GET", local, null).statusCode());
        assertEquals(409, enviar("POST", "/quartos", dados).statusCode());
        assertEquals(400, enviar("POST", "/quartos", "{}").statusCode());
        assertEquals(400, enviar("POST", "/quartos", "{\"numero\":0,\"andar\":0,\"capacidadeMaxima\":1}").statusCode());
        assertEquals(400, enviar("POST", "/quartos", "{quebrado").statusCode());
        assertEquals(400, enviar("GET", "/quartos?situacao=invalida", null).statusCode());
        assertEquals(400, enviar("GET", "/quartos/abc", null).statusCode());
        assertEquals("[]", enviar("GET", "/quartos?andar=99", null).body());
        service.registrarEntrada(id, 1001);
        service.registrarEntrada(id, 1002);
        assertTrue(enviar("GET", "/quartos?situacao=OCUPADO&numero=501&andar=5", null).body().contains("501"));
        assertEquals(409, enviar("DELETE", local, null).statusCode());
        assertEquals(409, enviar("PUT", local, dados.replace(":2", ":1")).statusCode());
        service.registrarAlta(id, 1001);
        service.registrarAlta(id, 1002);
        assertEquals(200, enviar("PUT", local, dados.replace("501", "502")).statusCode());
        assertEquals(204, enviar("DELETE", local, null).statusCode());
        assertEquals(404, enviar("GET", local, null).statusCode());
        assertEquals(404, enviar("PUT", local, dados).statusCode());
        assertEquals(404, enviar("DELETE", local, null).statusCode());
    }

    @Test void serveTemplateThymeleaf() throws Exception {
        var tela = enviar("GET", "/quartos.html", null);
        assertEquals(200, tela.statusCode());
        assertTrue(tela.body().contains("action=\"/quartos.html\""));
        assertFalse(tela.body().contains("th:each"));
        assertEquals(200, enviar("GET", "/css/style.css", null).statusCode());
    }
    @Test void apiRejeitaLimitesEFracoesSemModificarCadastro() throws Exception {
        String[] invalidos = {
            "{\"numero\":0,\"andar\":0,\"capacidadeMaxima\":1}",
            "{\"numero\":10000,\"andar\":0,\"capacidadeMaxima\":1}",
            "{\"numero\":8101,\"andar\":-3,\"capacidadeMaxima\":1}",
            "{\"numero\":8101,\"andar\":31,\"capacidadeMaxima\":1}",
            "{\"numero\":8101,\"andar\":0,\"capacidadeMaxima\":0}",
            "{\"numero\":8101,\"andar\":0,\"capacidadeMaxima\":21}",
            "{\"numero\":8101,\"andar\":0,\"capacidadeMaxima\":20.9}",
            "{\"numero\":8101.5,\"andar\":0,\"capacidadeMaxima\":1}",
            "{\"numero\":8101,\"andar\":30.9,\"capacidadeMaxima\":1}"
        };
        for (String dados : invalidos) {
            var erro = enviar("POST", "/quartos", dados);
            assertEquals(400, erro.statusCode(), dados + " => " + erro.body());
            assertTrue(erro.body().contains("mensagem"));
        }
        assertEquals("[]", enviar("GET", "/quartos?numero=8101", null).body());
        String dados = "{\"numero\":9999,\"andar\":30,\"capacidadeMaxima\":20}";
        var criado = enviar("POST", "/quartos", dados);
        assertEquals(201, criado.statusCode());
        String local = criado.headers().firstValue("Location").orElseThrow();
        var erro = enviar("PUT", local, dados.replace("30", "31"));
        assertEquals(400, erro.statusCode());
        assertTrue(erro.body().contains("Andar deve estar entre -2 e 30."));
        assertTrue(enviar("GET", local, null).body().contains("\"andar\":30"));
        assertEquals(204, enviar("DELETE", local, null).statusCode());
    }
}
