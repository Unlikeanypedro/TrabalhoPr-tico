package com.example.application;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.application.repository.ReferenciasConsultaEmMemoria;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfissionalApiTest {
    @LocalServerPort int porta;
    @Autowired ReferenciasConsultaEmMemoria referencias;
    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> enviar(String metodo, String caminho, String json) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
            .header("Content-Type", "application/json")
            .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void crudValidacoesETela() throws Exception {
        String dados = """
            {"nome":"Profissional API","registroProfissional":"CRM/MG API1","telefone":"31999999999",
             "email":"api@hospital.com","categoria":"ESPECIALISTA","especialidade":"Cardiologia"}
            """;
        var criado = enviar("POST", "/profissionais", dados);
        assertEquals(201, criado.statusCode(), criado.body());
        String local = criado.headers().firstValue("Location").orElseThrow();
        try {
            assertTrue(criado.body().contains("Cardiologia"));
            assertEquals(409, enviar("POST", "/profissionais", dados).statusCode());
            assertEquals(200, enviar("GET", local, null).statusCode());
            assertTrue(enviar("GET", "/profissionais?especialidade=Cardiologia&categoria=ESPECIALISTA", null).body().contains("CRM/MG API1"));
            assertEquals(200, enviar("PUT", local, dados.replace("Profissional API", "Nome editado")).statusCode());
            assertTrue(enviar("GET", local, null).body().contains("Nome editado"));
            assertEquals(400, enviar("PUT", local, dados.replace("api@hospital.com", "invalido")).statusCode());
            assertTrue(enviar("GET", local, null).body().contains("api@hospital.com"));
            referencias.registrarPaciente(90001);
            assertEquals(409, enviar("POST", local + "/procedimentos", "{\"pacienteId\":90001,\"dataHora\":\"2020-01-01T10:00:00\",\"descricao\":\"Cirurgia\"}").statusCode());
            assertEquals(409, enviar("POST", local + "/encaminhamentos", "{\"pacienteId\":90001,\"especialidade\":\"Ortopedia\",\"motivo\":\"Dor\"}").statusCode());
            assertEquals("[]", enviar("GET", local + "/consultas", null).body());
        } finally {
            assertEquals(204, enviar("DELETE", local, null).statusCode());
        }
        assertEquals(404, enviar("GET", local, null).statusCode());
        assertEquals(400, enviar("POST", "/profissionais", "{}").statusCode());
        assertEquals(400, enviar("POST", "/profissionais", "{quebrado").statusCode());
        assertEquals(400, enviar("GET", "/profissionais?categoria=invalida", null).statusCode());
        assertEquals(400, enviar("GET", "/profissionais/abc", null).statusCode());
        var tela = enviar("GET", "/profissionais.html", null);
        assertEquals(200, tela.statusCode());
        assertTrue(tela.body().contains("formulario-profissional"));
        assertEquals(200, enviar("GET", "/js/profissionais.js", null).statusCode());
    }

    @Test void registraProcedimentoSomenteParaCirurgiaoComPacienteExistente() throws Exception {
        var criado = enviar("POST", "/profissionais", """
            {"nome":"Cirurgião API","registroProfissional":"CRM/MG API2","telefone":"31",
             "email":"cirurgiao@hospital.com","categoria":"CIRURGIAO","especialidade":"Ortopedia","areaCirurgica":"Joelho"}
            """);
        assertEquals(201, criado.statusCode());
        String local = criado.headers().firstValue("Location").orElseThrow();
        String dados = "{\"pacienteId\":90002,\"dataHora\":\"2020-01-01T10:00:00\",\"descricao\":\"Artroscopia\"}";
        assertEquals(404, enviar("POST", local + "/procedimentos", dados).statusCode());
        referencias.registrarPaciente(90002);
        assertEquals(201, enviar("POST", local + "/procedimentos", dados).statusCode());
        assertTrue(enviar("GET", local + "/procedimentos", null).body().contains("Artroscopia"));
        assertEquals(409, enviar("DELETE", local, null).statusCode());
        assertEquals(400, enviar("POST", local + "/procedimentos", dados.replace("2020", "2999")).statusCode());
    }
}
