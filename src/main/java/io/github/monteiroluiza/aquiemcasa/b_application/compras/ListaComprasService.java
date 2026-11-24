package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.ItemLista;
import io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
import io.github.monteiroluiza.aquiemcasa.a_domain.CategoriaProduto;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ItemListaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ListaComprasRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;

@Service
public class ListaComprasService {
    private final AcessoCasaService acesso;
    private final ListaComprasRepository listas;
    private final ItemListaRepository itens;
    private final ProdutoEstoqueRepository produtos;

    public ListaComprasService(AcessoCasaService acesso, ListaComprasRepository listas,
                               ItemListaRepository itens, ProdutoEstoqueRepository produtos) {
        this.acesso = acesso;
        this.listas = listas;
        this.itens = itens;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public ListaComprasResponse consultar(UUID casaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = listas.findByCasaId(casaId).orElseThrow(this::listaNaoEncontrada);
        return resposta(lista);
    }

    @Transactional
    public ListaComprasResponse atualizarQuantidade(UUID casaId, UUID itemId, String email,
                                                    AtualizarQuantidadeItemRequest dados) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = bloquearLista(casaId, dados.versao());
        var item = buscarItem(lista, itemId);
        if (item.getQuantidade().compareTo(dados.quantidade()) != 0) {
            item.atualizarQuantidade(dados.quantidade());
            lista.avancarVersao();
            listas.saveAndFlush(lista);
        }
        return resposta(lista);
    }

    @Transactional
    public ListaComprasResponse atualizarSituacao(UUID casaId, UUID itemId, String email,
                                                  AtualizarSituacaoItemRequest dados) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = bloquearLista(casaId, dados.versao());
        var item = buscarItem(lista, itemId);
        if (item.getSituacao() != dados.situacao()) {
            item.atualizarSituacao(dados.situacao());
            lista.avancarVersao();
            listas.saveAndFlush(lista);
        }
        return resposta(lista);
    }

    @Transactional
    public ListaComprasResponse remover(UUID casaId, UUID itemId, String email, long versao) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = bloquearLista(casaId, versao);
        var item = buscarItem(lista, itemId);
        itens.delete(item);
        lista.avancarVersao();
        listas.saveAndFlush(lista);
        return resposta(lista);
    }

    @Transactional
    public ListaComprasResponse incluirManualmente(UUID casaId, String email, IncluirItemManualRequest dados) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = bloquearLista(casaId, dados.versao());
        var itensAtuais = itens.findByListaId(lista.getId());

        ProdutoEstoque produto = null;
        String nome = null;
        String unidade = null;
        CategoriaProduto categoria = null;
        if (dados.produtoId() != null) {
            if (temTexto(dados.nome()) || temTexto(dados.unidade()) || dados.categoria() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ao informar produtoId, não envie nome, unidade ou categoria.");
            }
            produto = produtos.buscarTodosParaLista(casaId, List.of(dados.produtoId())).stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto não encontrado nesta casa."));
        } else {
            if (!temTexto(dados.nome()) || !temTexto(dados.unidade())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe nome e unidade para cadastrar um item avulso.");
            }
            nome = dados.nome().strip();
            unidade = dados.unidade().strip();
            categoria = dados.categoria() == null ? CategoriaProduto.DESPENSA : dados.categoria();
            var compativeis = produtos.buscarCompativeis(casaId, nome, unidade);
            if (compativeis.size() > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Há mais de um produto compatível. Escolha um produto e envie seu produtoId.");
            }
            if (compativeis.size() == 1) {
                produto = compativeis.getFirst();
            }
        }

        ItemLista item = localizarLinha(itensAtuais, produto, nome, unidade);
        if (item == null) {
            item = produto != null
                    ? new ItemLista(lista.getCasa(), lista, produto, dados.quantidade())
                    : new ItemLista(lista.getCasa(), lista, nome, unidade, categoria, dados.quantidade());
            itens.save(item);
        } else {
            validarQuantidade(item.getQuantidade().add(dados.quantidade()));
            item.somarQuantidade(dados.quantidade());
        }
        lista.avancarVersao();
        listas.saveAndFlush(lista);
        return resposta(lista);
    }

    private ItemLista localizarLinha(List<ItemLista> itensAtuais, ProdutoEstoque produto,
                                     String nome, String unidade) {
        if (produto != null) {
            UUID produtoId = produto.getId();
            return itensAtuais.stream()
                    .filter(item -> item.getProduto() != null && item.getProduto().getId().equals(produtoId))
                    .findFirst().orElse(null);
        }
        String nomeNormalizado = normalizar(nome);
        String unidadeNormalizada = normalizar(unidade);
        return itensAtuais.stream()
                .filter(item -> item.getProduto() == null)
                .filter(item -> normalizar(item.getNomeExibicao()).equals(nomeNormalizado))
                .filter(item -> normalizar(item.getUnidadeExibicao()).equals(unidadeNormalizada))
                .findFirst().orElse(null);
    }

    private void validarQuantidade(java.math.BigDecimal quantidade) {
        if (quantidade.scale() > 3 || quantidade.precision() - quantidade.scale() > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A quantidade excede o limite permitido.");
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String normalizar(String valor) {
        return valor.strip().toLowerCase(Locale.ROOT);
    }

    private ListaCompras bloquearLista(UUID casaId, long versaoEsperada) {
        var lista = listas.buscarPorCasaParaAlteracao(casaId).orElseThrow(this::listaNaoEncontrada);
        if (lista.getVersao() != versaoEsperada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A lista foi alterada por outro morador. Atualize a lista antes de continuar.");
        }
        return lista;
    }

    private ItemLista buscarItem(ListaCompras lista, UUID itemId) {
        return itens.findByIdAndListaId(itemId, lista.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Item da lista não encontrado."));
    }

    private ListaComprasResponse resposta(ListaCompras lista) {
        List<ItemListaResponse> respostas = itens.findByListaId(lista.getId()).stream()
                .sorted(Comparator.comparing(ItemLista::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
                .map(ItemListaResponse::de)
                .toList();
        return new ListaComprasResponse(lista.getId(), lista.getVersao(), respostas);
    }

    private ResponseStatusException listaNaoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista de compras não encontrada.");
    }
}
