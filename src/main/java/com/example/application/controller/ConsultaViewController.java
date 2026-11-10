package com.example.application.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.application.dto.*;
import com.example.application.exception.*;
import com.example.application.model.*;
import com.example.application.service.ConsultaService;
import com.example.application.service.ReferenciasConsulta;

@Controller
@RequestMapping("/consultas.html")
public class ConsultaViewController {
    private static final int TAMANHO_PAGINA = 5;
    private final ConsultaService service;
    private final ReferenciasConsulta referencias;

    public ConsultaViewController(ConsultaService service, ReferenciasConsulta referencias) {
        this.service = service;
        this.referencias = referencias;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Long medicoId, @RequestParam(required = false) TipoConsulta tipo,
            @RequestParam(required = false) SituacaoConsulta situacao,
            @RequestParam(defaultValue = "data-asc") String ordem,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        return preencherPagina(model, data, medicoId, tipo, situacao, ordem, pagina);
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("formulario", new ConsultaForm());
        return preencherPagina(model);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable long id, Model model) {
        Consulta consulta = buscar(id);
        if (!editavel(consulta)) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Só é possível editar consultas agendadas ou confirmadas.");
        var dados = ConsultaResponse.de(consulta);
        var formulario = new ConsultaForm();
        formulario.setId(id);
        formulario.setTipo(dados.tipo());
        formulario.setDataHora(dados.dataHora());
        formulario.setPacienteId(dados.pacienteId());
        formulario.setMedicoId(dados.medicoId());
        formulario.setMotivo(dados.motivo());
        formulario.setEspecialidade(dados.especialidade());
        formulario.setPrioridade(dados.prioridade());
        model.addAttribute("formulario", formulario);
        return preencherPagina(model);
    }

    @PostMapping
    public String salvar(@ModelAttribute("formulario") ConsultaForm formulario, BindingResult resultado,
            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("erro", "Verifique os IDs, a data, o horário e as opções do formulário.");
            return preencherPagina(model);
        }
        try {
            if (formulario.getId() == null) {
                String especialidade = formulario.getEspecialidade();
                if (especialidade != null && especialidade.isBlank()) especialidade = null;
                var dados = new ConsultaRequest(formulario.getTipo(), formulario.getDataHora(), formulario.getPacienteId(),
                    formulario.getMedicoId(), formulario.getMotivo(), especialidade, formulario.getPrioridade());
                dados.validar();
                switch (dados.tipo()) {
                    case ROTINA -> service.agendarRotina(dados.dataHora(), dados.pacienteId(), dados.medicoId(), dados.motivo());
                    case ESPECIALIZADA -> service.agendarEspecializada(dados.dataHora(), dados.pacienteId(), dados.medicoId(), dados.motivo(), dados.especialidade());
                    case EMERGENCIA -> service.registrarEmergencia(dados.dataHora(), dados.pacienteId(), dados.medicoId(), dados.motivo(), dados.prioridade());
                }
            } else {
                var dados = new ConsultaAtualizacaoRequest(formulario.getDataHora(), formulario.getPacienteId(),
                    formulario.getMedicoId(), formulario.getMotivo());
                dados.validar();
                service.atualizar(formulario.getId(), dados.dataHora(), dados.pacienteId(), dados.medicoId(), dados.motivo());
            }
            redirect.addFlashAttribute("mensagem", "Consulta salva.");
            return "redirect:/consultas.html";
        } catch (IllegalArgumentException | ConflitoException | NaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            return preencherPagina(model);
        }
    }

    @GetMapping("/atendimento/{id}")
    public String atendimento(@PathVariable long id, Model model) {
        preencherAtendimento(id, model);
        return preencherPagina(model);
    }

    @PostMapping("/{id}/situacao")
    public String alterarSituacao(@PathVariable long id, @RequestParam(required = false) String situacao,
            Model model, RedirectAttributes redirect) {
        try {
            if (situacao == null || situacao.isBlank()) throw new IllegalArgumentException("Informe a situação da consulta.");
            service.alterarSituacao(id, SituacaoConsulta.valueOf(situacao));
            redirect.addFlashAttribute("mensagem", "Situação atualizada.");
            return "redirect:/consultas.html/atendimento/" + id;
        } catch (IllegalArgumentException | ConflitoException | NaoEncontradoException e) {
            model.addAttribute("erro", e instanceof IllegalArgumentException ? "Informe uma situação válida para a consulta." : e.getMessage());
            preencherAtendimento(id, model);
            return preencherPagina(model);
        }
    }

    @PostMapping("/{id}/observacoes")
    public String registrarObservacoes(@PathVariable long id, @RequestParam(required = false) String observacoes,
            Model model, RedirectAttributes redirect) {
        try {
            service.registrarObservacoes(id, observacoes);
            redirect.addFlashAttribute("mensagem", "Observações salvas.");
            return "redirect:/consultas.html/atendimento/" + id;
        } catch (IllegalArgumentException | ConflitoException | NaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            preencherAtendimento(id, model);
            model.addAttribute("observacoes", observacoes == null ? "" : observacoes);
            return preencherPagina(model);
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("mensagem", "Consulta excluída.");
        } catch (ConflitoException | NaoEncontradoException | IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/consultas.html";
    }

    private Consulta buscar(long id) {
        try { return service.buscar(id); }
        catch (NaoEncontradoException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }

    private static boolean editavel(Consulta c) {
        return c.getSituacao() == SituacaoConsulta.AGENDADA || c.getSituacao() == SituacaoConsulta.CONFIRMADA;
    }

    private void preencherAtendimento(long id, Model model) {
        Consulta consulta = buscar(id);
        model.addAttribute("atendimento", ConsultaResponse.de(consulta));
        model.addAttribute("proximasSituacoes", Arrays.stream(SituacaoConsulta.values())
            .filter(consulta.getSituacao()::permiteTransicaoPara).toList());
        model.addAttribute("permiteObservacoes", consulta.getSituacao() == SituacaoConsulta.EM_ATENDIMENTO
            || consulta.getSituacao() == SituacaoConsulta.CONCLUIDA);
        model.addAttribute("observacoes", consulta.getObservacoes());
    }

    private String preencherPagina(Model model) {
        return preencherPagina(model, null, null, null, null, "data-asc", 0);
    }

    private String preencherPagina(Model model, LocalDate data, Long medicoId, TipoConsulta tipo,
            SituacaoConsulta situacao, String ordem, int pagina) {
        String ordenacao = switch (ordem) {
            case "data-desc", "profissional-asc", "profissional-desc", "tipo-asc", "tipo-desc" -> ordem;
            default -> "data-asc";
        };
        Comparator<Consulta> comparador = switch (ordenacao) {
            case "data-desc" -> Comparator.comparing(Consulta::getDataHora).reversed();
            case "profissional-asc" -> Comparator.comparingLong(c -> c.getMedico().id());
            case "profissional-desc" -> Comparator.comparingLong((Consulta c) -> c.getMedico().id()).reversed();
            case "tipo-asc" -> Comparator.comparing(c -> c.getTipo().getDescricao());
            case "tipo-desc" -> Comparator.comparing((Consulta c) -> c.getTipo().getDescricao()).reversed();
            default -> Comparator.comparing(Consulta::getDataHora);
        };
        java.util.List<Consulta> filtradas;
        try { filtradas = service.listar(data, medicoId, tipo, situacao).stream()
            .sorted(comparador.thenComparingLong(Consulta::getId)).toList(); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
        int total = filtradas.size();
        int ultima = total == 0 ? 0 : (total - 1) / TAMANHO_PAGINA;
        int atual = Math.max(0, Math.min(pagina, ultima));
        int inicio = atual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, total);
        model.addAttribute("consultas", filtradas.subList(inicio, fim));
        model.addAttribute("medicos", referencias.listarMedicos());
        model.addAttribute("tipos", TipoConsulta.values());
        model.addAttribute("prioridades", PrioridadeAtendimento.values());
        model.addAttribute("filtroData", data);
        model.addAttribute("filtroMedico", medicoId);
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroSituacao", situacao);
        model.addAttribute("ordem", ordenacao);
        model.addAttribute("pagina", atual);
        model.addAttribute("ultimaPagina", ultima);
        model.addAttribute("total", total);
        model.addAttribute("inicio", total == 0 ? 0 : inicio + 1);
        model.addAttribute("fim", fim);
        return "consultas";
    }
}
