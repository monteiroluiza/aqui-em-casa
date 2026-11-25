package io.github.monteiroluiza.aquiemcasa.a_domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Casa casa;

    private String nome;
    private Instant criadaEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembroCasa criadaPor;

    protected Categoria() {}

    public Categoria(Casa casa, String nome, MembroCasa criadaPor) {
        this.casa = casa;
        this.nome = nome;
        this.criadaPor = criadaPor;
        this.criadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public Instant getCriadaEm() { return criadaEm; }
    public MembroCasa getCriadaPor() { return criadaPor; }

    public void renomear(String nome) {
        this.nome = nome;
    }
}
