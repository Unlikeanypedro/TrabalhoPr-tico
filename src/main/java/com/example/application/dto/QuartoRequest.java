package com.example.application.dto;

import com.example.application.utils.QuartoValidacao;

public class QuartoRequest {
    private Integer numero;
    private Integer andar;
    private Integer capacidadeMaxima;

    public QuartoRequest() {}

    public QuartoRequest(Integer numero, Integer andar, Integer capacidadeMaxima) {
        this.numero = numero;
        this.andar = andar;
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public Integer getAndar() { return andar; }
    public void setAndar(Integer andar) { this.andar = andar; }
    public Integer getCapacidadeMaxima() { return capacidadeMaxima; }
    public void setCapacidadeMaxima(Integer capacidadeMaxima) { this.capacidadeMaxima = capacidadeMaxima; }

    public void validar() {
        QuartoValidacao.validarDados(numero, andar, capacidadeMaxima);
    }
}
