package com.example.application;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.service.QuartoService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuartoViewTest {
    @LocalServerPort int porta;
    @Autowired QuartoService service;
    private final HttpClient cliente = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();

    private HttpResponse<String> enviar(String metodo, String caminho, String formulario) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .method(metodo, formulario == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(formulario))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void cadastroRenderizaDadosSemJavaScript() throws Exception {
        var resposta = enviar("POST", "/quartos.html", "numero=7001&andar=7&capacidadeMaxima=20");
        assertEquals(302, resposta.statusCode());
        assertEquals("/quartos.html", URI.create(resposta.headers().firstValue("Location").orElseThrow()).getPath().split(";")[0]);
        var pagina = enviar("GET", "/quartos.html?numero=7001", null);
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("<td>7001</td>"));
        assertTrue(pagina.body().contains("<td>0/20</td>"));
        assertTrue(pagina.body().contains("Quarto salvo."));
        assertFalse(pagina.body().contains("th:"));
        assertFalse(pagina.body().contains("js/quartos.js"));
    }

    @Test void edicaoPreencheFormularioEAtualizaMesmoId() throws Exception {
        long id = service.criar(7101, 7, 2).getId();
        var editar = enviar("GET", "/quartos.html/editar/" + id, null);
        assertEquals(200, editar.statusCode());
        assertTrue(editar.body().contains("Editar quarto 7101"));
        assertTrue(editar.body().contains("value=\"7101\""));
        var salvar = enviar("POST", "/quartos.html", "id=" + id + "&numero=7102&andar=0&capacidadeMaxima=3");
        assertEquals(302, salvar.statusCode());
        assertEquals(7102, service.buscar(id).getNumero());
        assertEquals(0, service.buscar(id).getAndar());
        assertEquals(3, service.buscar(id).getCapacidadeMaxima());
    }

    @Test void formularioInvalidoPreservaValoresSemGravar() throws Exception {
        var vazio = enviar("POST", "/quartos.html", "numero=7201&andar=7&capacidadeMaxima=");
        assertEquals(200, vazio.statusCode());
        assertTrue(vazio.body().contains("são obrigatórios"));
        assertTrue(vazio.body().contains("value=\"7201\""));
        assertTrue(service.listar(7201, null, null).isEmpty());
        var texto = enviar("POST", "/quartos.html", "numero=abc&andar=7&capacidadeMaxima=1");
        assertEquals(200, texto.statusCode());
        assertTrue(texto.body().contains("números inteiros válidos"));
        var negativo = enviar("POST", "/quartos.html", "numero=7202&andar=7&capacidadeMaxima=0");
        assertTrue(negativo.body().contains("Capacidade deve estar entre 1 e 20."));
        assertTrue(service.listar(7202, null, null).isEmpty());
    }

    @Test void numeroDuplicadoMostraErroNoFormulario() throws Exception {
        service.criar(7301, 7, 2);
        var resposta = enviar("POST", "/quartos.html", "numero=7301&andar=8&capacidadeMaxima=3");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("Já existe um quarto com esse número."));
        assertEquals(1, service.listar(7301, null, null).size());
    }

    @Test void filtrosOrdenacaoEPaginacaoSaoProcessadosNoServidor() throws Exception {
        for (int numero = 7401; numero <= 7407; numero++) service.criar(numero, 18, 2);
        long ocupado = service.listar(7407, null, null).getFirst().getId();
        service.registrarEntrada(ocupado, 740701);
        service.registrarEntrada(ocupado, 740702);
        var primeira = enviar("GET", "/quartos.html?andar=18&ordem=numero-desc", null).body();
        assertTrue(primeira.contains("Exibindo 1 a 5 de 7 quartos"));
        assertTrue(primeira.indexOf("<td>7407</td>") < primeira.indexOf("<td>7406</td>"));
        assertFalse(primeira.contains("<td>7401</td>"));
        var segunda = enviar("GET", "/quartos.html?andar=18&ordem=numero-desc&pagina=1", null).body();
        assertTrue(segunda.contains("Exibindo 6 a 7 de 7 quartos"));
        assertTrue(segunda.contains("<td>7401</td>"));
        assertFalse(segunda.contains("<td>7407</td>"));
        var filtro = enviar("GET", "/quartos.html?andar=18&situacao=OCUPADO", null).body();
        assertTrue(filtro.contains("Exibindo 1 a 1 de 1 quartos"));
        assertTrue(filtro.contains("<td>2/2</td>"));
        assertTrue(enviar("GET", "/quartos.html?andar=999", null).body().contains("Nenhum quarto encontrado."));
        assertTrue(enviar("GET", "/quartos.html?andar=18&pagina=-3", null).body().contains("Exibindo 1 a 5"));
        assertTrue(enviar("GET", "/quartos.html?andar=18&pagina=999", null).body().contains("Exibindo 6 a 7"));
    }

    @Test void exclusaoEEdicaoRespeitamOcupacao() throws Exception {
        long id = service.criar(7501, 7, 2).getId();
        service.registrarEntrada(id, 750101);
        service.registrarEntrada(id, 750102);
        var reduzir = enviar("POST", "/quartos.html", "id=" + id + "&numero=7501&andar=7&capacidadeMaxima=1");
        assertEquals(200, reduzir.statusCode());
        assertTrue(reduzir.body().contains("Capacidade não pode ser menor que a ocupação atual."));
        assertEquals(302, enviar("POST", "/quartos.html/" + id + "/excluir", "").statusCode());
        assertTrue(enviar("GET", "/quartos.html", null).body().contains("Não é possível excluir um quarto"));
        assertEquals(2, service.buscar(id).ocupacaoAtual());
        service.registrarAlta(id, 750101);
        service.registrarAlta(id, 750102);
        assertEquals(302, enviar("POST", "/quartos.html/" + id + "/excluir", "").statusCode());
        assertTrue(enviar("GET", "/quartos.html", null).body().contains("Quarto excluído."));
        assertThrows(NaoEncontradoException.class, () -> service.buscar(id));
    }

    @Test void recursosInexistentesEParametrosInvalidos() throws Exception {
        assertEquals(404, enviar("GET", "/quartos.html/editar/99999999", null).statusCode());
        assertEquals(400, enviar("GET", "/quartos.html?numero=abc", null).statusCode());
        assertEquals(400, enviar("GET", "/quartos.html?situacao=invalida", null).statusCode());
        assertEquals(200, enviar("GET", "/index.html", null).statusCode());
        assertEquals(200, enviar("GET", "/css/style.css", null).statusCode());
    }
    @Test void telaAplicaLimitesMesmoComFormularioForjado() throws Exception {
        String[][] invalidos = {
            {"numero=8201&andar=31&capacidadeMaxima=1", "Andar deve estar entre -2 e 30."},
            {"numero=8201&andar=-3&capacidadeMaxima=1", "Andar deve estar entre -2 e 30."},
            {"numero=8201&andar=1&capacidadeMaxima=21", "Capacidade deve estar entre 1 e 20."},
            {"numero=10000&andar=1&capacidadeMaxima=1", "Número do quarto deve estar entre 1 e 9999."}
        };
        for (String[] dados : invalidos) {
            var resposta = enviar("POST", "/quartos.html", dados[0]);
            assertEquals(200, resposta.statusCode());
            assertTrue(resposta.body().contains(dados[1]));
        }
        assertTrue(service.listar(8201, null, null).isEmpty());
        var pagina = enviar("GET", "/quartos.html", null).body();
        assertTrue(pagina.contains("min=\"-2\""));
        assertTrue(pagina.contains("max=\"30\""));
        assertTrue(pagina.contains("max=\"20\""));
        assertTrue(pagina.contains("max=\"9999\""));
        assertTrue(pagina.contains("value=\"DISPONIVEL\""));
        assertTrue(pagina.contains("value=\"OCUPADO\""));
    }
}
