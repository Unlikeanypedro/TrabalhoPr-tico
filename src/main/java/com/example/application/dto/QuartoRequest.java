package com.example.application.dto;

public record QuartoRequest(Integer numero, Integer andar, Integer capacidadeMaxima) {
    public void validar() {
        if (numero == null || andar == null || capacidadeMaxima == null)
            throw new IllegalArgumentException("Número, andar e capacidade máxima são obrigatórios.");
    }
}
