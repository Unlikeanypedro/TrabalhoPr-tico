package com.example.application.repository;

import com.example.application.model.Quarto;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuartoRepositoryJdbc implements QuartoRepository {
    private static final String CONSULTA_QUARTOS = """
        SELECT q.id, q.numero, q.andar, q.capacidade, o.internacao_id
        FROM sprint3_quarto q LEFT JOIN sprint3_ocupacao o ON o.quarto_id = q.id
        """;
    private final JdbcTemplate jdbc;

    public QuartoRepositoryJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bloquearAlteracoes() {
        jdbc.queryForObject("SELECT id FROM sprint3_controle_vagas WHERE id = 1 FOR UPDATE", Integer.class);
    }

    @Override
    public long proximoId() {
        return jdbc.queryForObject("SELECT nextval('sprint3_quarto_seq')", Long.class);
    }

    @Override
    public List<Quarto> listar() {
        return consultar(CONSULTA_QUARTOS);
    }

    @Override
    public Optional<Quarto> buscarPorId(long id) {
        return consultar(CONSULTA_QUARTOS + " WHERE q.id = ?", id).stream().findFirst();
    }

    private List<Quarto> consultar(String consulta, Object... parametros) {
        return jdbc.query(consulta, (ResultSet resultado) -> {
            Map<Long, Quarto> quartos = new LinkedHashMap<>();
            while (resultado.next()) {
                long id = resultado.getLong("id");
                Quarto quarto = quartos.get(id);
                if (quarto == null) {
                    quarto = new Quarto(id, resultado.getInt("numero"), resultado.getInt("andar"),
                        resultado.getInt("capacidade"));
                    quartos.put(id, quarto);
                }
                long internacaoId = resultado.getLong("internacao_id");
                if (!resultado.wasNull()) {
                    quarto.alocarPaciente(internacaoId);
                }
            }
            return List.copyOf(quartos.values());
        }, parametros);
    }

    @Override
    public void salvar(Quarto quarto) {
        int alterados = jdbc.update("UPDATE sprint3_quarto SET numero = ?, andar = ?, capacidade = ? WHERE id = ?",
            quarto.getNumero(), quarto.getAndar(), quarto.getCapacidadeMaxima(), quarto.getId());
        if (alterados == 0) {
            jdbc.update("INSERT INTO sprint3_quarto (id, numero, andar, capacidade) VALUES (?, ?, ?, ?)",
                quarto.getId(), quarto.getNumero(), quarto.getAndar(), quarto.getCapacidadeMaxima());
        }
        jdbc.update("DELETE FROM sprint3_ocupacao WHERE quarto_id = ?", quarto.getId());
        for (long internacaoId : quarto.getInternacoesAtivas()) {
            jdbc.update("INSERT INTO sprint3_ocupacao (internacao_id, quarto_id) VALUES (?, ?)",
                internacaoId, quarto.getId());
        }
    }

    @Override
    public void excluir(long id) {
        jdbc.update("DELETE FROM sprint3_quarto WHERE id = ?", id);
    }
}
