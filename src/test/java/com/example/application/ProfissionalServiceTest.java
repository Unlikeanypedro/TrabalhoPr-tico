package com.example.application;

import com.example.application.dto.ProfissionalRequest;
import com.example.application.exception.*;
import com.example.application.model.*;
import com.example.application.repository.*;
import com.example.application.service.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfissionalServiceTest {
    private ProfissionalService service;
    private ReferenciasConsultaEmMemoria referencias;
    private ConsultaService consultas;

    @BeforeEach void preparar() {
        var repository = new ProfissionalRepositoryEmMemoria();
        var consultasRepository = new ConsultaRepositoryEmMemoria();
        referencias = new ReferenciasConsultaEmMemoria(repository);
        referencias.registrarPaciente(10);
        service = new ProfissionalService(repository, referencias, consultasRepository);
        consultas = new ConsultaService(consultasRepository, referencias);
    }

    private ProfissionalRequest dados(String nome, String registro, CategoriaProfissional categoria, String especialidade, String area) {
        return new ProfissionalRequest(nome, registro, "31999999999", "medico@hospital.com", categoria, especialidade, area);
    }

    @Test void categoriasFiltrosERegistroUnico() {
        var clinico = service.criar(dados("Ana", "CRM/MG 1", CategoriaProfissional.CLINICO_GERAL, null, null));
        var especialista = service.criar(dados("Bruno", "CRM/MG 2", CategoriaProfissional.ESPECIALISTA, "Cardiologia", null));
        var cirurgiao = service.criar(dados("Carla", "CRM/MG 3", CategoriaProfissional.CIRURGIAO, "Ortopedia", "Joelho"));
        assertInstanceOf(ClinicoGeral.class, clinico);
        assertInstanceOf(MedicoEspecialista.class, especialista);
        assertInstanceOf(Cirurgiao.class, cirurgiao);
        assertEquals(1, service.listar("BRU", null, " cardiologia ").size());
        assertEquals(1, service.listar(null, CategoriaProfissional.CIRURGIAO, null).size());
        assertThrows(ConflitoException.class, () -> service.criar(dados("Outro", " crm/mg 1 ", CategoriaProfissional.CLINICO_GERAL, null, null)));
        assertThrows(ConflitoException.class, () -> service.atualizar(cirurgiao.getId(), dados("Carla", "CRM/MG 2", CategoriaProfissional.CIRURGIAO, "Ortopedia", "Joelho")));
        assertEquals("CRM/MG 3", service.buscar(cirurgiao.getId()).getRegistroProfissional());
        service.excluir(especialista.getId());
        assertTrue(referencias.buscarMedico(especialista.getId()).isEmpty());
    }

    @Test void validaDadosComunsEEspecificos() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(dados(" ", "1", CategoriaProfissional.CLINICO_GERAL, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.criar(dados("Ana", "1", CategoriaProfissional.ESPECIALISTA, " ", null)));
        assertThrows(IllegalArgumentException.class, () -> service.criar(dados("Ana", "1", CategoriaProfissional.CIRURGIAO, "Ortopedia", null)));
        assertThrows(IllegalArgumentException.class, () -> service.criar(dados("Ana", "1", CategoriaProfissional.CLINICO_GERAL, "Cardiologia", null)));
        assertThrows(IllegalArgumentException.class, () -> service.criar(new ProfissionalRequest("Ana", "1", "31", "invalido", CategoriaProfissional.CLINICO_GERAL, null, null)));
        assertTrue(service.listar(null, null, null).isEmpty());
    }

    @Test void encaminhamentoEProcedimentoRespeitamCategoriaEPaciente() {
        var clinico = service.criar(dados("Ana", "1", CategoriaProfissional.CLINICO_GERAL, null, null));
        var cirurgiao = service.criar(dados("Bia", "2", CategoriaProfissional.CIRURGIAO, "Ortopedia", "Joelho"));
        var encaminhamento = new Encaminhamento(10, "Ortopedia", "Dor no joelho");
        var procedimento = new ProcedimentoProfissional(10, LocalDateTime.now().minusDays(1), "Artroscopia");
        assertThrows(ConflitoException.class, () -> service.encaminhar(cirurgiao.getId(), encaminhamento));
        assertThrows(ConflitoException.class, () -> service.registrarProcedimento(clinico.getId(), procedimento));
        assertThrows(NaoEncontradoException.class, () -> service.encaminhar(clinico.getId(), new Encaminhamento(999, "Ortopedia", "Dor")));
        assertThrows(NaoEncontradoException.class, () -> service.registrarProcedimento(cirurgiao.getId(), new ProcedimentoProfissional(999, procedimento.dataHora(), "Cirurgia")));
        service.encaminhar(clinico.getId(), encaminhamento);
        service.registrarProcedimento(cirurgiao.getId(), procedimento);
        assertEquals(encaminhamento, service.listarEncaminhamentos(clinico.getId()).getFirst());
        assertEquals(procedimento, service.listarProcedimentos(cirurgiao.getId()).getFirst());
        assertThrows(UnsupportedOperationException.class, () -> service.listarProcedimentos(cirurgiao.getId()).clear());
        assertThrows(ConflitoException.class, () -> service.excluir(cirurgiao.getId()));
        assertThrows(ConflitoException.class, () -> service.atualizar(clinico.getId(), dados("Ana", "1", CategoriaProfissional.ESPECIALISTA, "Cardiologia", null)));
        assertThrows(IllegalArgumentException.class, () -> new ProcedimentoProfissional(10, LocalDateTime.now().plusDays(1), "Cirurgia futura"));
    }

    @Test void cadastroIntegraConsultasEProtegeHistorico() {
        var medico = service.criar(dados("Ana", "1", CategoriaProfissional.ESPECIALISTA, "Cardiologia", null));
        assertEquals("Cardiologia", referencias.buscarMedico(medico.getId()).orElseThrow().especialidade());
        assertEquals(1, referencias.listarMedicos().size());
        assertThrows(IllegalArgumentException.class, () -> consultas.agendarEspecializada(LocalDateTime.now(), 10, medico.getId(), "Dor", "Ortopedia"));
        var consulta = consultas.agendarEspecializada(LocalDateTime.now(), 10, medico.getId(), "Retorno", "Cardiologia");
        assertEquals(consulta.getId(), service.listarConsultas(medico.getId()).getFirst().getId());
        assertThrows(ConflitoException.class, () -> service.excluir(medico.getId()));
        assertThrows(ConflitoException.class, () -> service.atualizar(medico.getId(), dados("Ana", "1", CategoriaProfissional.ESPECIALISTA, "Ortopedia", null)));
        service.atualizar(medico.getId(), dados("Ana Silva", "1", CategoriaProfissional.ESPECIALISTA, "Cardiologia", null));
        assertEquals("Ana Silva", service.buscar(medico.getId()).getNome());
    }
}
