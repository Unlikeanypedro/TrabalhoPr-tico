package com.example.application;

import com.example.application.model.ClinicoGeral;
import com.example.application.model.Paciente;
import com.example.application.repository.PacienteRepository;
import com.example.application.repository.ProfissionalRepository;

final class CadastrosInternacaoTeste {
    private CadastrosInternacaoTeste() {}

    static void paciente(PacienteRepository repositorio, long id) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setNome("Paciente " + id);
        repositorio.salvar(paciente);
    }

    static void profissional(ProfissionalRepository repositorio, long id) {
        repositorio.salvar(new ClinicoGeral(id, "Médico " + id, "CRM-" + id, "31999999999",
            "medico" + id + "@exemplo.com"));
    }
}
