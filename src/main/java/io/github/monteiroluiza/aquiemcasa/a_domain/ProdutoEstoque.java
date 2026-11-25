package io.github.monteiroluiza.aquiemcasa.a_domain;

import java.math.BigDecimal;
import java.time.Instant;
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
@Table(name = "produto_estoque")
public class ProdutoEstoque {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Casa casa;

    private String nome;

    @Enumerated(EnumType.STRING)
    private CategoriaProduto categoria;

    private String unidade;
    private BigDecimal quantidadeAtual;
    private BigDecimal quantidadeDesejada;
    private Instant atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembroCasa atualizadoPor;

    protected ProdutoEstoque() {}

    public ProdutoEstoque(Casa casa, String nome, CategoriaProduto categoria, String unidade,
                          BigDecimal quantidadeAtual, BigDecimal quantidadeDesejada, MembroCasa atualizadoPor) {
        this.casa = casa;
        this.nome = nome;
        this.categoria = categoria;
        this.unidade = unidade;
        this.quantidadeAtual = quantidadeAtual;
        this.quantidadeDesejada = quantidadeDesejada;
        this.atualizadoPor = atualizadoPor;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public CategoriaProduto getCategoria() { return categoria; }
    public String getUnidade() { return unidade; }
    public BigDecimal getQuantidadeAtual() { return quantidadeAtual; }
    public BigDecimal getQuantidadeDesejada() { return quantidadeDesejada; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public MembroCasa getAtualizadoPor() { return atualizadoPor; }

    public SituacaoEstoque getSituacao() {
        if (quantidadeAtual.signum() == 0) return SituacaoEstoque.EM_FALTA;
        if (quantidadeAtual.compareTo(quantidadeDesejada) < 0) return SituacaoEstoque.BAIXO;
        return SituacaoEstoque.EM_DIA;
    }

    public BigDecimal getQuantidadeFaltante() {
        return quantidadeDesejada.subtract(quantidadeAtual).max(BigDecimal.ZERO);
    }

    public void atualizar(String nome, CategoriaProduto categoria, String unidade,
                          BigDecimal quantidadeAtual, BigDecimal quantidadeDesejada, MembroCasa autor) {
        this.nome = nome;
        this.categoria = categoria;
        this.unidade = unidade;
        this.quantidadeAtual = quantidadeAtual;
        this.quantidadeDesejada = quantidadeDesejada;
        registrarAlteracao(autor);
    }

    public void ajustarQuantidade(BigDecimal novaQuantidade, MembroCasa autor) {
        this.quantidadeAtual = novaQuantidade;
        registrarAlteracao(autor);
    }

    public void receberCompra(BigDecimal quantidade, MembroCasa autor) {
        this.quantidadeAtual = this.quantidadeAtual.add(quantidade);
        registrarAlteracao(autor);
    }

    private void registrarAlteracao(MembroCasa autor) {
        this.atualizadoPor = autor;
        this.atualizadoEm = Instant.now();
    }
}
