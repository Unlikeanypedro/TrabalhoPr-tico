package com.exemplo.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity // Define que esta classe é uma entidade JPA (mapeada para uma tabela)
@Table(name = "tb_produtos") // Opcional: define o nome exato da tabela no banco
public class Produto {

    @Id // Define a chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incremento no banco
    private Long id;

    @Column(nullable = false, length = 100) // Configurações da coluna
    private String nome;

    @Column(nullable = false)
    private Double preco;

    // Construtor padrão (obrigatório para o JPA)
    public Produto() {
    }

    // Construtor completo
    public Produto(Long id, String nome, Double preco) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }
}