package com.example.application.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.application.dto.AltaRequest;
import com.example.application.dto.AtualizacaoInternacaoRequest;
import com.example.application.dto.InternacaoRequest;
import com.example.application.dto.InternacaoResponse;
import com.example.application.dto.TransferenciaRequest;
import com.example.application.model.SituacaoInternacao;
import com.example.application.service.InternacaoService;

/**
 * GET   /internacoes                     lista (filtros opcionais: pacienteId, profissionalId, quartoId, situacao)
 * GET   /internacoes/{id}                busca uma internação
 * POST  /internacoes                     registra
 * PUT   /internacoes/{id}                altera profissional, data prevista de alta e observações
 * PATCH /internacoes/{id}/transferencia  troca de quarto
 * PATCH /internacoes/{id}/alta           registra a alta e libera a vaga
 * Não há DELETE: internação faz parte do histórico do paciente.
 */
@RestController
@RequestMapping("/internacoes")
public class InternacaoController {
    private final InternacaoService service;
    public InternacaoController(InternacaoService service) { this.service = service; }

    @GetMapping
    public List<InternacaoResponse> listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) Long quartoId,
            @RequestParam(required = false) SituacaoInternacao situacao) {
        return service.listar(pacienteId, profissionalId, quartoId, situacao).stream()
            .map(internacao -> InternacaoResponse.de(internacao)).toList();
    }

    @GetMapping("/{id}")
    public InternacaoResponse buscar(@PathVariable long id) { return InternacaoResponse.de(service.buscar(id)); }

    @PostMapping
    public ResponseEntity<InternacaoResponse> registrar(@RequestBody InternacaoRequest dados) {
        var internacao = service.registrar(dados.getPacienteId(), dados.getProfissionalId(), dados.getQuartoId(),
            dados.getDataEntrada(), dados.getDataPrevistaAlta(), dados.getObservacoes());
        return ResponseEntity.created(URI.create("/internacoes/" + internacao.getId()))
            .body(InternacaoResponse.de(internacao));
    }

    @PutMapping("/{id}")
    public InternacaoResponse atualizar(@PathVariable long id, @RequestBody AtualizacaoInternacaoRequest dados) {
        return InternacaoResponse.de(service.atualizar(id, dados.getProfissionalId(),
            dados.getDataPrevistaAlta(), dados.getObservacoes()));
    }

    @PatchMapping("/{id}/transferencia")
    public InternacaoResponse transferir(@PathVariable long id, @RequestBody TransferenciaRequest dados) {
        return InternacaoResponse.de(service.transferir(id, dados.getQuartoId()));
    }

    @PatchMapping("/{id}/alta")
    public InternacaoResponse darAlta(@PathVariable long id, @RequestBody AltaRequest dados) {
        return InternacaoResponse.de(service.darAlta(id, dados.getDataEfetivaAlta(), dados.getObservacoes()));
    }
}
