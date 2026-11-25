package com.example.application.repository;

import com.example.application.model.EstadiaQuarto;
import com.example.application.model.Internacao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InternacaoRepositoryJdbc implements InternacaoRepository {
    private static final String CONSULTA_INTERNACOES = """
        SELECT i.*, e.ordem, e.quarto_id AS estadia_quarto_id, e.numero_quarto,
            e.data_entrada AS estadia_entrada, e.data_saida AS estadia_saida
        FROM sprint3_internacao i LEFT JOIN sprint3_estadia e ON e.internacao_id = i.id
        """;

    private final JdbcTemplate jdbc;

    public InternacaoRepositoryJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long proximoId() {
        return jdbc.queryForObject("SELECT nextval('sprint3_internacao_seq')", Long.class);
    }

    @Override
    public List<Internacao> listar() {
        return consultar(CONSULTA_INTERNACOES);
    }

    @Override
    public Optional<Internacao> buscarPorId(long id) {
        return consultar(CONSULTA_INTERNACOES + " WHERE i.id = ?", id).stream().findFirst();
    }

    private List<Internacao> consultar(String consulta, Object... parametros) {
        return jdbc.query(consulta + " ORDER BY i.id, e.ordem", (ResultSet rs) -> {
            List<Internacao> internacoes = new ArrayList<>();
            boolean temRegistro = rs.next();
            while (temRegistro) {
                long id = rs.getLong("id");
                long pacienteId = rs.getLong("paciente_id");
                long profissionalId = rs.getLong("profissional_id");
                long quartoId = rs.getLong("quarto_id");
                LocalDate entrada = data(rs, "data_entrada");
                LocalDate prevista = data(rs, "data_prevista_alta");
                LocalDate alta = data(rs, "data_efetiva_alta");
                String observacoes = rs.getString("observacoes");
                List<EstadiaQuarto> estadias = new ArrayList<>();
                do {
                    if (rs.getObject("ordem") != null) {
                        estadias.add(new EstadiaQuarto(rs.getLong("estadia_quarto_id"), rs.getInt("numero_quarto"),
                            data(rs, "estadia_entrada"), data(rs, "estadia_saida")));
                    }
                    temRegistro = rs.next();
                } while (temRegistro && rs.getLong("id") == id);
                internacoes.add(Internacao.restaurar(id, pacienteId, profissionalId, quartoId, entrada,
                    prevista, alta, observacoes, estadias));
            }
            return internacoes;
        }, parametros);
    }

    private LocalDate data(ResultSet resultado, String coluna) throws SQLException {
        return resultado.getObject(coluna, LocalDate.class);
    }

    @Override
    public void salvar(Internacao internacao) {
        Long id = internacao.getId();
        int alterados = jdbc.update("""
            UPDATE sprint3_internacao
            SET profissional_id = ?, quarto_id = ?, data_prevista_alta = ?, data_efetiva_alta = ?, observacoes = ?
            WHERE id = ?
            """, internacao.getProfissionalId(), internacao.getQuartoId(), internacao.getDataPrevistaAlta(),
            internacao.getDataEfetivaAlta(), internacao.getObservacoes(), id);
        if (alterados == 0) {
            jdbc.update("""
                INSERT INTO sprint3_internacao
                    (id, paciente_id, profissional_id, quarto_id, data_entrada, data_prevista_alta,
                     data_efetiva_alta, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, internacao.getPacienteId(), internacao.getProfissionalId(), internacao.getQuartoId(),
                internacao.getDataEntrada(), internacao.getDataPrevistaAlta(), internacao.getDataEfetivaAlta(),
                internacao.getObservacoes());
        }
        jdbc.update("DELETE FROM sprint3_estadia WHERE internacao_id = ?", id);
        int ordem = 0;
        for (EstadiaQuarto estadia : internacao.getHistoricoQuartos()) {
            jdbc.update("""
                INSERT INTO sprint3_estadia (internacao_id, ordem, quarto_id, numero_quarto, data_entrada, data_saida)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, ordem++, estadia.getQuartoId(), estadia.getNumeroQuarto(), estadia.getDataEntrada(),
                estadia.getDataSaida());
        }
    }
}
