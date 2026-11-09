package com.example.application.exception;

public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String mensagem) { super(mensagem); }
}
