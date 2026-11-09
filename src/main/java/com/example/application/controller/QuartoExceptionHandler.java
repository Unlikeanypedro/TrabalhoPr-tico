package com.example.application.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;

@RestControllerAdvice(assignableTypes = QuartoController.class)
public class QuartoExceptionHandler {
    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(NaoEncontradoException e) {
        return erro(404, e.getMessage());
    }
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, String>> conflito(ConflitoException e) {
        return erro(409, e.getMessage());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return erro(400, e.getMessage());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> formato(Exception e) {
        return erro(400, "Requisição inválida. Verifique o JSON, os números e a situação (DISPONIVEL ou OCUPADO).");
    }
    private ResponseEntity<Map<String, String>> erro(int status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("mensagem", mensagem));
    }
}
