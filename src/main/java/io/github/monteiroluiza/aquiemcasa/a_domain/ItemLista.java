package io.github.monteiroluiza.aquiemcasa.a_domain;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_lista")
public class ItemLista {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Casa casa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ListaCompras lista;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProdutoEstoque produto;

    private String nome;
    private String unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    private Categoria categoria;

    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    private SituacaoItemLista situacao;

    protected ItemLista() {}

    public ItemLista(Casa casa, ListaCompras lista, ProdutoEstoque produto, BigDecimal quantidade) {
        this.casa = casa;
        this.lista = lista;
        this.produto = produto;
        this.quantidade = quantidade;
        this.situacao = SituacaoItemLista.PENDENTE;
    }

    public ItemLista(Casa casa, ListaCompras lista, String nome, String unidade,
                     Categoria categoria, BigDecimal quantidade) {
        this.casa = casa;
        this.lista = lista;
        this.nome = nome;
        this.unidade = unidade;
        this.categoria = categoria;
        this.quantidade = quantidade;
        this.situacao = SituacaoItemLista.PENDENTE;
    }

    public UUID getId() { return id; }
    public ProdutoEstoque getProduto() { return produto; }
    public BigDecimal getQuantidade() { return quantidade; }
    public SituacaoItemLista getSituacao() { return situacao; }
    public String getNomeExibicao() { return produto == null ? nome : produto.getNome(); }
    public String getUnidadeExibicao() { return produto == null ? unidade : produto.getUnidade(); }
    public Categoria getCategoriaExibicao() { return produto == null ? categoria : produto.getCategoria(); }

    public void atualizarQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public void somarQuantidade(BigDecimal quantidade) {
        this.quantidade = this.quantidade.add(quantidade);
    }

    public void atualizarSituacao(SituacaoItemLista situacao) {
        this.situacao = situacao;
    }
}
