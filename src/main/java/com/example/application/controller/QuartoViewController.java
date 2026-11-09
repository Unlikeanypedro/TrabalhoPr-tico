package com.example.application.controller;

import java.util.Comparator;
import java.util.Map;
import com.example.application.utils.QuartoConstantes;
import static com.example.application.utils.QuartoConstantes.TAMANHO_PAGINA;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.application.dto.QuartoForm;
import com.example.application.dto.QuartoRequest;
import com.example.application.dto.QuartoResponse;
import com.example.application.exception.ConflitoException;
import com.example.application.exception.NaoEncontradoException;
import com.example.application.model.Quarto;
import com.example.application.model.SituacaoQuarto;
import com.example.application.service.QuartoService;

@Controller
@RequestMapping("/quartos.html")
public class QuartoViewController {
    private final QuartoService service;

    public QuartoViewController(QuartoService service) { this.service = service; }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer numero,
            @RequestParam(required = false) Integer andar,
            @RequestParam(required = false) SituacaoQuarto situacao,
            @RequestParam(defaultValue = "numero-asc") String ordem,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        return preencherPagina(model, new QuartoForm(), numero, andar, situacao, ordem, pagina);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable long id, Model model) {
        Quarto quarto = buscar(id);
        QuartoForm formulario = new QuartoForm();
        formulario.setId(quarto.getId());
        formulario.setNumero(quarto.getNumero());
        formulario.setAndar(quarto.getAndar());
        formulario.setCapacidadeMaxima(quarto.getCapacidadeMaxima());
        return preencherPagina(model, formulario, null, null, null, "numero-asc", 0);
    }

    @PostMapping
    public String salvar(@ModelAttribute("formulario") QuartoForm formulario,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("erro", "Informe números inteiros válidos nos campos do formulário.");
            return preencherPagina(model, formulario, null, null, null, "numero-asc", 0);
        }
        try {
            QuartoRequest dados = new QuartoRequest(formulario.getNumero(), formulario.getAndar(),
                formulario.getCapacidadeMaxima());
            dados.validar();
            if (formulario.getId() == null) {
                service.criar(dados.getNumero(), dados.getAndar(), dados.getCapacidadeMaxima());
            } else {
                service.atualizar(formulario.getId(), dados.getNumero(), dados.getAndar(), dados.getCapacidadeMaxima());
            }
            redirect.addFlashAttribute("mensagem", "Quarto salvo.");
            return "redirect:/quartos.html";
        } catch (IllegalArgumentException | ConflitoException | NaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            return preencherPagina(model, formulario, null, null, null, "numero-asc", 0);
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("mensagem", "Quarto excluído.");
        } catch (ConflitoException | NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/quartos.html";
    }

    private Quarto buscar(long id) {
        try { return service.buscar(id); }
        catch (NaoEncontradoException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private String preencherPagina(Model model, QuartoForm formulario, Integer numero,
            Integer andar, SituacaoQuarto situacao, String ordem, int pagina) {
        String ordenacao = switch (ordem) {
            case "numero-desc", "andar" -> ordem;
            default -> "numero-asc";
        };
        Comparator<Quarto> comparador = switch (ordenacao) {
            case "numero-desc" -> Comparator.comparingInt((Quarto quarto) -> quarto.getNumero()).reversed();
            case "andar" -> Comparator.comparingInt((Quarto quarto) -> quarto.getAndar()).thenComparingInt(quarto -> quarto.getNumero());
            default -> Comparator.comparingInt((Quarto quarto) -> quarto.getNumero());
        };
        var filtrados = service.listar(numero, andar, situacao).stream().sorted(comparador).toList();
        int total = filtrados.size();
        int ultimaPagina = total == 0 ? 0 : (total - 1) / TAMANHO_PAGINA;
        int atual = Math.max(0, Math.min(pagina, ultimaPagina));
        int inicio = atual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, total);
        model.addAttribute("quartos", filtrados.subList(inicio, fim).stream().map(quarto -> QuartoResponse.de(quarto)).toList());
        model.addAttribute("formulario", formulario);
        model.addAttribute("andares", service.listar(null, null, null).stream()
            .map(quarto -> quarto.getAndar()).distinct().sorted().toList());
        model.addAttribute("filtroNumero", numero);
        model.addAttribute("filtroAndar", andar);
        model.addAttribute("filtroSituacao", situacao);
        model.addAttribute("situacoes", SituacaoQuarto.values());
        model.addAttribute("situacaoDisponivel", SituacaoQuarto.DISPONIVEL);
        model.addAttribute("limites", Map.of(
            "numeroMinimo", QuartoConstantes.NUMERO_MINIMO,
            "numeroMaximo", QuartoConstantes.NUMERO_MAXIMO,
            "andarMinimo", QuartoConstantes.ANDAR_MINIMO,
            "andarMaximo", QuartoConstantes.ANDAR_MAXIMO,
            "capacidadeMinima", QuartoConstantes.CAPACIDADE_MINIMA,
            "capacidadeMaxima", QuartoConstantes.CAPACIDADE_MAXIMA));
        model.addAttribute("ordem", ordenacao);
        model.addAttribute("pagina", atual);
        model.addAttribute("ultimaPagina", ultimaPagina);
        model.addAttribute("total", total);
        model.addAttribute("inicio", total == 0 ? 0 : inicio + 1);
        model.addAttribute("fim", fim);
        return "quartos";
    }
}
