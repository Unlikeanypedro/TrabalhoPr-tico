package com.example.application.dto;

import com.example.application.model.Quarto;
import com.example.application.model.SituacaoQuarto;

public record QuartoResponse(long id, int numero, int andar, int capacidadeMaxima,
                             int ocupacaoAtual, SituacaoQuarto situacao) {
    public static QuartoResponse de(Quarto q) {
        return new QuartoResponse(q.getId(), q.getNumero(), q.getAndar(),
            q.getCapacidadeMaxima(), q.ocupacaoAtual(), q.getSituacao());
    }
}
