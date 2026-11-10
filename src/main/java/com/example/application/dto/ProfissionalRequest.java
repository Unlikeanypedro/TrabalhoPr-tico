package com.example.application.dto;

import com.example.application.model.*;

public record ProfissionalRequest(String nome, String registroProfissional, String telefone, String email,
        CategoriaProfissional categoria, String especialidade, String areaCirurgica) {
    public Profissional criar(long id) {
        if (categoria == null) throw new IllegalArgumentException("Informe a categoria profissional.");
        if (categoria == CategoriaProfissional.CLINICO_GERAL && especialidade != null && !especialidade.isBlank())
            throw new IllegalArgumentException("Clínico geral não possui especialidade cadastrada.");
        if (categoria != CategoriaProfissional.CIRURGIAO && areaCirurgica != null && !areaCirurgica.isBlank())
            throw new IllegalArgumentException("Área cirúrgica é exclusiva de cirurgiões.");
        return switch (categoria) {
            case CLINICO_GERAL -> new ClinicoGeral(id, nome, registroProfissional, telefone, email);
            case ESPECIALISTA -> new MedicoEspecialista(id, nome, registroProfissional, telefone, email, especialidade);
            case CIRURGIAO -> new Cirurgiao(id, nome, registroProfissional, telefone, email, especialidade, areaCirurgica);
        };
    }
}
