package com.example.application.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.application.dto.QuartoRequest;
import com.example.application.dto.QuartoResponse;
import com.example.application.model.SituacaoQuarto;
import com.example.application.service.QuartoService;

@RestController
@RequestMapping("/quartos")
public class QuartoController {
    private final QuartoService service;
    public QuartoController(QuartoService service) { this.service = service; }

    @GetMapping
    public List<QuartoResponse> listar(@RequestParam(required = false) Integer numero,
            @RequestParam(required = false) Integer andar,
            @RequestParam(required = false) SituacaoQuarto situacao) {
        return service.listar(numero, andar, situacao).stream().map(QuartoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public QuartoResponse buscar(@PathVariable long id) { return QuartoResponse.de(service.buscar(id)); }

    @PostMapping
    public ResponseEntity<QuartoResponse> criar(@RequestBody QuartoRequest dados) {
        dados.validar();
        var quarto = service.criar(dados.numero(), dados.andar(), dados.capacidadeMaxima());
        return ResponseEntity.created(URI.create("/quartos/" + quarto.getId())).body(QuartoResponse.de(quarto));
    }

    @PutMapping("/{id}")
    public QuartoResponse atualizar(@PathVariable long id, @RequestBody QuartoRequest dados) {
        dados.validar();
        return QuartoResponse.de(service.atualizar(id, dados.numero(), dados.andar(), dados.capacidadeMaxima()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
