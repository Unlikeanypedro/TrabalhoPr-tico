package com.example.application.controller;

import static com.example.application.utils.InternacaoConstantes.TAMANHO_PAGINA;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.application.dto.AltaForm;
import com.example.application.dto.InternacaoForm;
import com.example.application.dto.InternacaoResponse;
import com.example.application.dto.QuartoResponse;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.model.Internacao;
import com.example.application.model.SituacaoInternacao;
import com.example.application.service.InternacaoService;
import com.example.application.service.QuartoService;
import com.example.application.utils.InternacaoConstantes;

@Controller
@RequestMapping("/internacoes.html")
public class InternacaoViewController {
    private final InternacaoService service;
    private final QuartoService quartoService;

    public InternacaoViewController(InternacaoService service, QuartoService quartoService) {
        this.service = service;
        this.quartoService = quartoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) Long quartoId,
            @RequestParam(required = false) SituacaoInternacao situacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entradaDesde,
            @RequestParam(defaultValue = "entrada-desc") String ordem,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        Filtros filtros = new Filtros(pacienteId, profissionalId, quartoId, situacao, entradaDesde, ordem, pagina);
        return preencherPagina(model, new InternacaoForm(), novaAlta(null), filtros);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable long id, Model model, RedirectAttributes redirect) {
        Internacao internacao = buscar(id);
        if (!internacao.estaAtiva()) {
            redirect.addFlashAttribute("erro", "Internações finalizadas não podem ser editadas.");
            return "redirect:/internacoes.html";
        }
        InternacaoForm formulario = new InternacaoForm();
        formulario.setId(internacao.getId());
        formulario.setPacienteId(internacao.getPacienteId());
        formulario.setProfissionalId(internacao.getProfissionalId());
        formulario.setQuartoId(internacao.getQuartoId());
        formulario.setDataEntrada(internacao.getDataEntrada());
        formulario.setDataPrevistaAlta(internacao.getDataPrevistaAlta());
        formulario.setObservacoes(internacao.getObservacoes());
        return preencherPagina(model, formulario, novaAlta(null), Filtros.vazios());
    }

    @GetMapping("/alta/{id}")
    public String prepararAlta(@PathVariable long id, Model model) {
        buscar(id);
        return preencherPagina(model, new InternacaoForm(), novaAlta(id), Filtros.vazios());
    }

    @PostMapping
    public String salvar(@ModelAttribute("formulario") InternacaoForm formulario,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("erro", "Informe IDs (números inteiros) e datas válidas no formulário.");
            return preencherPagina(model, formulario, novaAlta(null), Filtros.vazios());
        }
        try {
            if (formulario.getId() == null) {
                service.registrar(formulario.getPacienteId(), formulario.getProfissionalId(),
                    formulario.getQuartoId(), formulario.getDataEntrada(), formulario.getDataPrevistaAlta(),
                    formulario.getObservacoes());
                redirect.addFlashAttribute("mensagem", "Internação registrada.");
            } else {
                service.editar(formulario.getId(), formulario.getProfissionalId(),
                    formulario.getDataPrevistaAlta(), formulario.getObservacoes(), formulario.getQuartoId());
                redirect.addFlashAttribute("mensagem", "Internação atualizada.");
            }
            return "redirect:/internacoes.html";
        } catch (IllegalArgumentException | ConflitoException | NaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            return preencherPagina(model, formulario, novaAlta(null), Filtros.vazios());
        }
    }

    @PostMapping("/alta")
    public String darAlta(@ModelAttribute("altaForm") AltaForm altaForm,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("erro", "Informe uma data de alta válida.");
            return preencherPagina(model, new InternacaoForm(), altaForm, Filtros.vazios());
        }
        try {
            if (altaForm.getInternacaoId() == null)
                throw new IllegalArgumentException("Selecione a internação que vai receber alta.");
            service.darAlta(altaForm.getInternacaoId(), altaForm.getDataEfetivaAlta(), altaForm.getObservacoes());
            redirect.addFlashAttribute("mensagem", "Alta registrada.");
            return "redirect:/internacoes.html";
        } catch (IllegalArgumentException | ConflitoException | NaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            return preencherPagina(model, new InternacaoForm(), altaForm, Filtros.vazios());
        }
    }

    private Internacao buscar(long id) {
        try { return service.buscar(id); }
        catch (NaoEncontradoException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private AltaForm novaAlta(Long internacaoId) {
        AltaForm altaForm = new AltaForm();
        altaForm.setInternacaoId(internacaoId);
        altaForm.setDataEfetivaAlta(LocalDate.now());
        return altaForm;
    }

    private String preencherPagina(Model model, InternacaoForm formulario, AltaForm altaForm, Filtros filtros) {
        String ordenacao = switch (filtros.ordem()) {
            case "entrada-asc", "alta-prevista", "paciente" -> filtros.ordem();
            default -> "entrada-desc";
        };
        Comparator<Internacao> porEntrada = Comparator.comparing((Internacao i) -> i.getDataEntrada())
            .thenComparingLong(i -> i.getId());
        Comparator<Internacao> comparador = switch (ordenacao) {
            case "entrada-asc" -> porEntrada;
            case "alta-prevista" -> Comparator.comparing((Internacao i) -> i.getDataPrevistaAlta())
                .thenComparingLong(i -> i.getId());
            case "paciente" -> Comparator.comparingLong((Internacao i) -> i.getPacienteId())
                .thenComparing(porEntrada.reversed());
            default -> porEntrada.reversed();
        };
        var filtradas = service.listar(filtros.pacienteId(), filtros.profissionalId(), filtros.quartoId(),
                filtros.situacao()).stream()
            .filter(i -> filtros.entradaDesde() == null || !i.getDataEntrada().isBefore(filtros.entradaDesde()))
            .sorted(comparador).toList();
        int total = filtradas.size();
        int ultimaPagina = total == 0 ? 0 : (total - 1) / TAMANHO_PAGINA;
        int atual = Math.max(0, Math.min(filtros.pagina(), ultimaPagina));
        int inicio = atual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, total);

        List<QuartoResponse> quartos = quartoService.listar(null, null, null).stream()
            .map(quarto -> QuartoResponse.de(quarto)).toList();
        Map<Long, Integer> numerosDosQuartos = quartos.stream()
            .collect(Collectors.toMap(quarto -> quarto.getId(), quarto -> quarto.getNumero()));

        model.addAttribute("internacoes", filtradas.subList(inicio, fim).stream()
            .map(internacao -> InternacaoResponse.de(internacao)).toList());
        model.addAttribute("ativas", service.listar(null, null, null, SituacaoInternacao.ATIVA).stream()
            .map(internacao -> InternacaoResponse.de(internacao)).toList());
        model.addAttribute("quartos", quartos);
        model.addAttribute("numerosDosQuartos", numerosDosQuartos);
        model.addAttribute("formulario", formulario);
        model.addAttribute("altaForm", altaForm);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("maximoObservacoes", InternacaoConstantes.TAMANHO_MAXIMO_OBSERVACOES);
        model.addAttribute("situacoes", SituacaoInternacao.values());
        model.addAttribute("situacaoAtiva", SituacaoInternacao.ATIVA);
        model.addAttribute("filtroPaciente", filtros.pacienteId());
        model.addAttribute("filtroProfissional", filtros.profissionalId());
        model.addAttribute("filtroQuarto", filtros.quartoId());
        model.addAttribute("filtroSituacao", filtros.situacao());
        model.addAttribute("filtroEntrada", filtros.entradaDesde());
        model.addAttribute("ordem", ordenacao);
        model.addAttribute("pagina", atual);
        model.addAttribute("ultimaPagina", ultimaPagina);
        model.addAttribute("total", total);
        model.addAttribute("inicio", total == 0 ? 0 : inicio + 1);
        model.addAttribute("fim", fim);
        return "internacoes";
    }

    private record Filtros(Long pacienteId, Long profissionalId, Long quartoId, SituacaoInternacao situacao,
            LocalDate entradaDesde, String ordem, int pagina) {
        static Filtros vazios() { return new Filtros(null, null, null, null, null, "entrada-desc", 0); }
    }
}
