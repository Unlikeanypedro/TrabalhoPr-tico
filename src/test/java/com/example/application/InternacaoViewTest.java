package com.example.application;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.application.service.InternacaoService;
import com.example.application.service.QuartoService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternacaoViewTest {
    @LocalServerPort int porta;
    @Autowired QuartoService quartos;
    @Autowired InternacaoService service;
    private final LocalDate hoje = LocalDate.now();
    private final HttpClient cliente = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();

    private HttpResponse<String> enviar(String metodo, String caminho, String formulario) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .method(metodo, formulario == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(formulario))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void registrarPelaTelaEDarAlta() throws Exception {
        long quarto = quartos.criar(6101, 6, 2).getId();
        var resposta = enviar("POST", "/internacoes.html", "pacienteId=920001&profissionalId=5&quartoId=" + quarto
            + "&dataEntrada=" + hoje.minusDays(1) + "&dataPrevistaAlta=" + hoje.plusDays(4) + "&observacoes=Teste");
        assertEquals(302, resposta.statusCode(), resposta.body());
        var pagina = enviar("GET", "/internacoes.html?pacienteId=920001", null);
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("Internação registrada."));
        assertTrue(pagina.body().contains("<td>920001</td>"));
        assertTrue(pagina.body().contains("<td>6101</td>"));
        assertTrue(pagina.body().contains(hoje.minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        assertFalse(pagina.body().contains("th:"));
        assertEquals(1, quartos.buscar(quarto).ocupacaoAtual());

        long id = service.listar(920001L, null, null, null).get(0).getId();
        var alta = enviar("POST", "/internacoes.html/alta", "internacaoId=" + id + "&dataEfetivaAlta=" + hoje);
        assertEquals(302, alta.statusCode(), alta.body());
        assertFalse(service.buscar(id).estaAtiva());
        assertEquals(0, quartos.buscar(quarto).ocupacaoAtual());
        var depois = enviar("GET", "/internacoes.html?pacienteId=920001", null).body();
        assertTrue(depois.contains("Alta registrada."));
        assertFalse(depois.contains("/internacoes.html/alta/" + id));
    }

    @Test void edicaoPreencheFormularioETransfere() throws Exception {
        long origem = quartos.criar(6102, 6, 2).getId();
        long destino = quartos.criar(6103, 6, 2).getId();
        long id = service.registrar(920002L, 5L, origem, hoje, hoje.plusDays(2), null).getId();
        var editar = enviar("GET", "/internacoes.html/editar/" + id, null);
        assertEquals(200, editar.statusCode());
        assertTrue(editar.body().contains("Editar internação " + id));
        assertTrue(editar.body().contains("value=\"920002\""));
        var salvar = enviar("POST", "/internacoes.html", "id=" + id + "&pacienteId=920002&profissionalId=6&quartoId="
            + destino + "&dataEntrada=" + hoje + "&dataPrevistaAlta=" + hoje.plusDays(6));
        assertEquals(302, salvar.statusCode(), salvar.body());
        assertEquals(destino, service.buscar(id).getQuartoId());
        assertEquals(6, service.buscar(id).getProfissionalId());
        assertEquals(1, quartos.buscar(destino).ocupacaoAtual());
        assertEquals(0, quartos.buscar(origem).ocupacaoAtual());
    }

    @Test void formularioInvalidoMostraErroSemGravar() throws Exception {
        long quarto = quartos.criar(6104, 6, 1).getId();
        var semData = enviar("POST", "/internacoes.html", "pacienteId=920003&profissionalId=5&quartoId=" + quarto
            + "&dataEntrada=&dataPrevistaAlta=");
        assertEquals(200, semData.statusCode());
        assertTrue(semData.body().contains("Informe a data de entrada."));
        assertTrue(semData.body().contains("value=\"920003\""));
        var futura = enviar("POST", "/internacoes.html", "pacienteId=920003&profissionalId=5&quartoId=" + quarto
            + "&dataEntrada=" + hoje.plusDays(1) + "&dataPrevistaAlta=" + hoje.plusDays(2));
        assertTrue(futura.body().contains("não pode ser futura"));
        var letras = enviar("POST", "/internacoes.html", "pacienteId=abc&profissionalId=5&quartoId=" + quarto);
        assertEquals(200, letras.statusCode());
        assertTrue(letras.body().contains("Informe IDs"));
        assertTrue(service.listar(920003L, null, null, null).isEmpty());
        assertEquals(0, quartos.buscar(quarto).ocupacaoAtual());
        assertEquals(404, enviar("GET", "/internacoes.html/editar/999999", null).statusCode());
    }
}
