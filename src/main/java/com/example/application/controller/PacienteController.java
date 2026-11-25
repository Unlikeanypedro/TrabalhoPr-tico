package com.example.application.controller;

import com.example.application.model.Paciente;
import com.example.application.service.PacienteService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService service;
    public PacienteController(PacienteService service) { this.service = service; }

    @GetMapping
    public List<Paciente> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Paciente buscar(@PathVariable long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Paciente> criar(@RequestBody Paciente dados) {
        var paciente = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/pacientes/" + paciente.getId())).body(paciente);
    }

    @PutMapping("/{id}")
    public Paciente atualizar(@PathVariable long id, @RequestBody Paciente dados) {
        dados.setId(id);
        return service.cadastrar(dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}