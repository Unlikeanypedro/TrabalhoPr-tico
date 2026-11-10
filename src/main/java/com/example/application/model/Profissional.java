package com.example.application.model;

public abstract class Profissional {
    private final long id;
    private final String nome;
    private final String registroProfissional;
    private final String telefone;
    private final String email;

    protected Profissional(long id, String nome, String registroProfissional, String telefone, String email) {
        if (id <= 0) throw new IllegalArgumentException("ID do profissional deve ser positivo.");
        this.id = id;
        this.nome = texto(nome, "Nome");
        this.registroProfissional = texto(registroProfissional, "Registro profissional");
        this.telefone = texto(telefone, "Telefone");
        this.email = texto(email, "E-mail");
        if (!this.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("E-mail inválido.");
    }

    protected static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " deve ser informado.");
        return valor.strip();
    }

    public abstract CategoriaProfissional getCategoria();
    public abstract String getEspecialidade();
    public String getAreaCirurgica() { return null; }
    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getRegistroProfissional() { return registroProfissional; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
}
