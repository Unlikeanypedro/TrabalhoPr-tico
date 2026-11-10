package com.example.application.controller;

import com.example.application.dto.*;
import com.example.application.model.*;
import com.example.application.service.ProfissionalService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profissionais")
public class ProfissionalController {
    private final ProfissionalService service;
    public ProfissionalController(ProfissionalService service) { this.service = service; }

    @GetMapping
    public List<Profissional> listar(@RequestParam(required = false) String nome,
            @RequestParam(required = false) CategoriaProfissional categoria,
            @RequestParam(required = false) String especialidade) {
        return service.listar(nome, categoria, especialidade);
    }
    @GetMapping("/{id}")
    public Profissional buscar(@PathVariable long id) { return service.buscar(id); }
    @PostMapping
    public ResponseEntity<Profissional> criar(@RequestBody ProfissionalRequest dados) {
        var profissional = service.criar(dados);
        return ResponseEntity.created(URI.create("/profissionais/" + profissional.getId())).body(profissional);
    }
    @PutMapping("/{id}")
    public Profissional atualizar(@PathVariable long id, @RequestBody ProfissionalRequest dados) { return service.atualizar(id, dados); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/consultas")
    public List<ConsultaResponse> consultas(@PathVariable long id) {
        return service.listarConsultas(id).stream().map(ConsultaResponse::de).toList();
    }
    @GetMapping("/{id}/encaminhamentos")
    public List<Encaminhamento> encaminhamentos(@PathVariable long id) { return service.listarEncaminhamentos(id); }
    @PostMapping("/{id}/encaminhamentos")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Encaminhamento encaminhar(@PathVariable long id, @RequestBody Encaminhamento dados) { return service.encaminhar(id, dados); }
    @GetMapping("/{id}/procedimentos")
    public List<ProcedimentoProfissional> procedimentos(@PathVariable long id) { return service.listarProcedimentos(id); }
    @PostMapping("/{id}/procedimentos")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProcedimentoProfissional registrar(@PathVariable long id, @RequestBody ProcedimentoProfissional dados) {
        return service.registrarProcedimento(id, dados);
    }
}
