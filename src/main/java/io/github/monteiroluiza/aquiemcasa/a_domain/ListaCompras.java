package io.github.monteiroluiza.aquiemcasa.a_domain;

import java.util.UUID;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Version;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lista_compras")
public class ListaCompras {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Casa casa;

    @Version
    private long versao;

    protected ListaCompras() {}

    public ListaCompras(Casa casa) {
        this.casa = casa;
    }

    public UUID getId() {
        return id;
    }
}
