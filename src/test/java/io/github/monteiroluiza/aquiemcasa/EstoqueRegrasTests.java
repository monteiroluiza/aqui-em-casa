package io.github.monteiroluiza.aquiemcasa;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;
import io.github.monteiroluiza.aquiemcasa.a_domain.*;
import io.github.monteiroluiza.aquiemcasa.b_application.estoque.*;
import io.github.monteiroluiza.aquiemcasa.b_application.casa.AcessoCasaService;
import io.github.monteiroluiza.aquiemcasa.b_application.categoria.CategoriaService;
import io.github.monteiroluiza.aquiemcasa.c_infrastructure.repositorio.ProdutoEstoqueRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
class EstoqueRegrasTests {
    @Test void compraAcimaDoMinimoPreservaMeta() {
        var produto=new ProdutoEstoque(null,"Leite",null,"caixa",BigDecimal.ZERO,new BigDecimal("2"),null);
        produto.receberCompra(new BigDecimal("3"),null);
        assertEquals(new BigDecimal("3"),produto.getQuantidadeAtual());
        assertEquals(new BigDecimal("2"),produto.getQuantidadeDesejada());
        assertEquals(BigDecimal.ZERO,produto.getQuantidadeFaltante());
        assertEquals(SituacaoEstoque.EM_DIA,produto.getSituacao());
        produto.ajustarQuantidade(BigDecimal.ONE,null);
        assertEquals(BigDecimal.ONE,produto.getQuantidadeFaltante());
        assertEquals(SituacaoEstoque.BAIXO,produto.getSituacao());
    }
    @Test void consumoNaoPermiteSaldoNegativo() {
        var repo=mock(ProdutoEstoqueRepository.class);
        var acesso=mock(AcessoCasaService.class);
        var jdbc=mock(JdbcClient.class,RETURNS_DEEP_STUBS);
        var service=new EstoqueService(repo,acesso,mock(CategoriaService.class),jdbc);
        UUID casa=UUID.randomUUID(), id=UUID.randomUUID();
        var produto=new ProdutoEstoque(null,"Leite",null,"caixa",BigDecimal.ONE,new BigDecimal("2"),null);
        when(repo.buscarParaAlteracao(casa,id)).thenReturn(Optional.of(produto));
        var erro=assertThrows(ResponseStatusException.class,() -> service.consumir(casa,id,"morador",new ConsumirProdutoRequest(new BigDecimal("2"))));
        assertEquals(400,erro.getStatusCode().value());
        assertEquals(BigDecimal.ONE,produto.getQuantidadeAtual());
        assertEquals(new BigDecimal("2"),produto.getQuantidadeDesejada());
        verify(acesso).exigirMembroAtivo(casa,"morador");
    }
    @Test void acessoRecusadoNaoConsultaEstoque() {
        var repo=mock(ProdutoEstoqueRepository.class);
        var acesso=mock(AcessoCasaService.class);
        UUID casa=UUID.randomUUID();
        when(acesso.exigirMembroAtivo(casa,"externo")).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        var service=new EstoqueService(repo,acesso,mock(CategoriaService.class),mock(JdbcClient.class));
        assertThrows(ResponseStatusException.class,() -> service.alertas(casa,"externo"));
        verifyNoInteractions(repo);
    }
}
