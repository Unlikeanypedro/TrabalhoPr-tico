package com.example.application.model;

import java.time.LocalDate;

public final class EstadiaQuarto {
    private final long quartoId;
    private final int numeroQuarto;
    private final LocalDate dataEntrada;
    private final LocalDate dataSaida;

    public EstadiaQuarto(long quartoId, int numeroQuarto, LocalDate dataEntrada, LocalDate dataSaida) {
        this.quartoId = quartoId;
        this.numeroQuarto = numeroQuarto;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
    }

    public long getQuartoId() { return quartoId; }
    public int getNumeroQuarto() { return numeroQuarto; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public LocalDate getDataSaida() { return dataSaida; }
}
