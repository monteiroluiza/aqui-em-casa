package io.github.monteiroluiza.aquiemcasa.a_domain;

import java.util.UUID;
import java.time.Instant;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "membro_casa")
public class MembroCasa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Casa casa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private Papel papel;

    private Instant entrouEm;
    private Instant removidoEm;

    protected MembroCasa() {}

    public MembroCasa(Casa casa, Usuario usuario, Papel papel) {
        this.casa = casa;
        this.usuario = usuario;
        this.papel = papel;
        this.entrouEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }
}
