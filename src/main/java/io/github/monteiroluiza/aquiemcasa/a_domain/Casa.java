package io.github.monteiroluiza.aquiemcasa.a_domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "casa")
public class Casa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    protected Casa() {}

    public Casa(String nome) {
        this.nome = nome;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
