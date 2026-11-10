package com.example.application.controller;

import com.example.application.dto.*;
import com.example.application.model.*;
import com.example.application.service.ConsultaService;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService service;

    public ConsultaController(ConsultaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsultaResponse> listar(
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate data,
        @RequestParam(required = false) Long medicoId,
        @RequestParam(required = false) TipoConsulta tipo,
        @RequestParam(required = false) SituacaoConsulta situacao
    ) {
        return service
            .listar(data, medicoId, tipo, situacao)
            .stream()
            .map(ConsultaResponse::de)
            .toList();
    }

    @GetMapping("/{id}")
    public ConsultaResponse buscar(@PathVariable long id) {
        return ConsultaResponse.de(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<ConsultaResponse> criar(
        @RequestBody ConsultaRequest dados
    ) {
        dados.validar();
        Consulta consulta = switch (dados.tipo()) {
            case ROTINA -> service.agendarRotina(
                dados.dataHora(),
                dados.pacienteId(),
                dados.medicoId(),
                dados.motivo()
            );
            case ESPECIALIZADA -> service.agendarEspecializada(
                dados.dataHora(),
                dados.pacienteId(),
                dados.medicoId(),
                dados.motivo(),
                dados.especialidade()
            );
            case EMERGENCIA -> service.registrarEmergencia(
                dados.dataHora(),
                dados.pacienteId(),
                dados.medicoId(),
                dados.motivo(),
                dados.prioridade()
            );
        };
        return ResponseEntity.created(
            URI.create("/consultas/" + consulta.getId())
        ).body(ConsultaResponse.de(consulta));
    }

    @PutMapping("/{id}")
    public ConsultaResponse atualizar(
        @PathVariable long id,
        @RequestBody ConsultaAtualizacaoRequest dados
    ) {
        dados.validar();
        return ConsultaResponse.de(
            service.atualizar(
                id,
                dados.dataHora(),
                dados.pacienteId(),
                dados.medicoId(),
                dados.motivo()
            )
        );
    }

    @PatchMapping("/{id}/situacao")
    public ConsultaResponse alterarSituacao(
        @PathVariable long id,
        @RequestBody ConsultaSituacaoRequest dados
    ) {
        dados.validar();
        return ConsultaResponse.de(
            service.alterarSituacao(id, dados.situacao())
        );
    }

    @PatchMapping("/{id}/observacoes")
    public ConsultaResponse registrarObservacoes(
        @PathVariable long id,
        @RequestBody ConsultaObservacoesRequest dados
    ) {
        dados.validar();
        return ConsultaResponse.de(
            service.registrarObservacoes(id, dados.observacoes())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
