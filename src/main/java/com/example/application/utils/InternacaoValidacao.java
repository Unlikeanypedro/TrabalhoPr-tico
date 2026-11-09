package com.example.application.utils;

import java.time.LocalDate;

public final class InternacaoValidacao {
    private InternacaoValidacao() {}

    public static void validarIdentificador(Long id, String campo) {
        if (id == null) throw new IllegalArgumentException("Informe o " + campo + ".");
        if (id <= 0) throw new IllegalArgumentException("ID do " + campo + " deve ser positivo.");
    }

    public static void validarDataEntrada(LocalDate dataEntrada) {
        if (dataEntrada == null) throw new IllegalArgumentException("Informe a data de entrada.");
        if (dataEntrada.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("A data de entrada não pode ser futura.");
    }

    public static void validarDataPrevistaAlta(LocalDate dataEntrada, LocalDate dataPrevistaAlta) {
        if (dataPrevistaAlta == null) throw new IllegalArgumentException("Informe a data prevista de alta.");
        if (dataPrevistaAlta.isBefore(dataEntrada))
            throw new IllegalArgumentException("A data prevista de alta não pode ser anterior à data de entrada.");
    }

    public static void validarDataAlta(LocalDate dataEntrada, LocalDate dataAlta) {
        if (dataAlta == null) throw new IllegalArgumentException("Informe a data efetiva de alta.");
        if (dataAlta.isBefore(dataEntrada))
            throw new IllegalArgumentException("A data de alta não pode ser anterior à data de entrada.");
        if (dataAlta.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("A data de alta não pode ser futura.");
    }

    /** Observações são opcionais. Devolve o texto sem espaços nas pontas, ou "" se vier vazio. */
    public static String normalizarObservacoes(String observacoes) {
        if (observacoes == null || observacoes.isBlank()) return "";
        String texto = observacoes.strip();
        if (texto.length() > InternacaoConstantes.TAMANHO_MAXIMO_OBSERVACOES)
            throw new IllegalArgumentException("As observações podem ter no máximo "
                + InternacaoConstantes.TAMANHO_MAXIMO_OBSERVACOES + " caracteres.");
        return texto;
    }
}
