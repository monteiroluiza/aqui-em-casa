package io.github.monteiroluiza.aquiemcasa.b_application.compras;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.ItemLista;
import io.github.monteiroluiza.aquiemcasa.a_domain.ProdutoEstoque;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ItemListaRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ListaComprasRepository;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;

@Service
public class GerarListaService {
    private final AcessoCasaService acesso;
    private final ListaComprasRepository listas;
    private final ItemListaRepository itens;
    private final ProdutoEstoqueRepository produtos;

    public GerarListaService(AcessoCasaService acesso, ListaComprasRepository listas,
                             ItemListaRepository itens, ProdutoEstoqueRepository produtos) {
        this.acesso = acesso;
        this.listas = listas;
        this.itens = itens;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public PreviaListaResponse gerarPrevia(UUID casaId, String email) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = listas.findByCasaId(casaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lista de compras não encontrada."));
        Map<UUID, ItemLista> itensPorProduto = itens.findByListaIdAndProdutoIsNotNull(lista.getId()).stream()
                .collect(Collectors.toMap(item -> item.getProduto().getId(), Function.identity()));
        var sugestoes = produtos.findByCasaIdOrderByCategoriaAscNomeAsc(casaId).stream()
                .filter(produto -> produto.getQuantidadeFaltante().signum() > 0)
                .map(produto -> criarSugestao(produto, itensPorProduto.get(produto.getId())))
                .toList();
        return new PreviaListaResponse(lista.getId(), lista.getVersao(), sugestoes);
    }

    private SugestaoItemResponse criarSugestao(ProdutoEstoque produto, ItemLista item) {
        BigDecimal faltante = produto.getQuantidadeFaltante();
        BigDecimal naLista = item == null ? BigDecimal.ZERO : item.getQuantidade();
        BigDecimal sugerida = faltante.max(naLista);
        AcaoSugestao acao = item == null
                ? AcaoSugestao.ADICIONAR
                : naLista.compareTo(faltante) < 0 ? AcaoSugestao.ATUALIZAR : AcaoSugestao.MANTER;
        return new SugestaoItemResponse(produto.getId(), produto.getNome(), produto.getCategoria(),
                produto.getUnidade(), faltante, naLista, sugerida, acao);
    }

    @Transactional
    public ListaComprasResponse confirmar(UUID casaId, String email, ConfirmarSugestoesRequest dados) {
        acesso.exigirMembroAtivo(casaId, email);
        var lista = listas.buscarPorCasaParaAlteracao(casaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lista de compras não encontrada."));
        if (lista.getVersao() != dados.versao()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A lista foi alterada. Gere uma nova prévia antes de confirmar.");
        }

        var produtoIds = dados.itens().stream().map(SugestaoSelecionadaRequest::produtoId).toList();
        if (new HashSet<>(produtoIds).size() != produtoIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Um produto não pode aparecer mais de uma vez na confirmação.");
        }
        Map<UUID, ProdutoEstoque> produtosSelecionados = produtos.buscarTodosParaLista(casaId, produtoIds).stream()
                .collect(Collectors.toMap(ProdutoEstoque::getId, Function.identity()));
        if (produtosSelecionados.size() != produtoIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Um ou mais produtos não pertencem a esta casa.");
        }

        Map<UUID, ItemLista> existentes = itens.findByListaIdAndProdutoIsNotNull(lista.getId()).stream()
                .collect(Collectors.toMap(item -> item.getProduto().getId(), Function.identity()));
        boolean alterou = false;
        for (var selecionado : dados.itens()) {
            var item = existentes.get(selecionado.produtoId());
            if (item == null) {
                item = new ItemLista(lista.getCasa(), lista, produtosSelecionados.get(selecionado.produtoId()),
                        selecionado.quantidade());
                itens.save(item);
                existentes.put(selecionado.produtoId(), item);
                alterou = true;
            } else if (item.getQuantidade().compareTo(selecionado.quantidade()) != 0) {
                item.atualizarQuantidade(selecionado.quantidade());
                alterou = true;
            }
        }
        if (alterou) {
            lista.avancarVersao();
            listas.saveAndFlush(lista);
        }
        return resposta(lista, existentes.values().stream().toList());
    }

    private ListaComprasResponse resposta(io.github.monteiroluiza.aquiemcasa.a_domain.ListaCompras lista,
                                          List<ItemLista> itensAtuais) {
        var respostas = itensAtuais.stream()
                .sorted(Comparator.comparing(item -> item.getProduto().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(ItemListaResponse::de)
                .toList();
        return new ListaComprasResponse(lista.getId(), lista.getVersao(), respostas);
    }
}
