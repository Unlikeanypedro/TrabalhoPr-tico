package com.example.application.exception;

public class QuartoSemVagasException extends ConflitoException {
    public QuartoSemVagasException() {
        super("Quarto sem vagas disponíveis.");
    }
}
