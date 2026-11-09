package com.example.application.utils;

import com.example.application.exception.DadosQuartoInvalidosException;

public final class QuartoValidacao {
    private QuartoValidacao() {}

    public static void validarDados(Integer numero, Integer andar, Integer capacidade) {
        if (numero == null || andar == null || capacidade == null)
            throw new DadosQuartoInvalidosException("Número, andar e capacidade máxima são obrigatórios.");
        validarIntervalo(numero, QuartoConstantes.NUMERO_MINIMO, QuartoConstantes.NUMERO_MAXIMO, "Número do quarto");
        validarIntervalo(andar, QuartoConstantes.ANDAR_MINIMO, QuartoConstantes.ANDAR_MAXIMO, "Andar");
        validarIntervalo(capacidade, QuartoConstantes.CAPACIDADE_MINIMA, QuartoConstantes.CAPACIDADE_MAXIMA, "Capacidade");
    }

    public static void validarIdentificador(long id, String campo) {
        if (id <= 0) throw new DadosQuartoInvalidosException(campo + " deve ser positivo.");
    }

    private static void validarIntervalo(int valor, int minimo, int maximo, String campo) {
        if (valor < minimo || valor > maximo)
            throw new DadosQuartoInvalidosException(campo + " deve estar entre " + minimo + " e " + maximo + ".");
    }
}
