package com.example.application.dto;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import com.example.application.model.PrioridadeAtendimento;
import com.example.application.model.TipoConsulta;

public class ConsultaForm {
    private Long id;
    private TipoConsulta tipo = TipoConsulta.ROTINA;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataHora;
    private Long pacienteId;
    private Long medicoId;
    private String motivo;
    private String especialidade;
    private PrioridadeAtendimento prioridade;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoConsulta getTipo() { return tipo; }
    public void setTipo(TipoConsulta tipo) { this.tipo = tipo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getMedicoId() { return medicoId; }
    public void setMedicoId(Long medicoId) { this.medicoId = medicoId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }
    public PrioridadeAtendimento getPrioridade() { return prioridade; }
    public void setPrioridade(PrioridadeAtendimento prioridade) { this.prioridade = prioridade; }
}
