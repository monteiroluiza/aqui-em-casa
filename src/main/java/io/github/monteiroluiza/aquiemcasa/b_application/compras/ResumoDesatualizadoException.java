package io.github.monteiroluiza.aquiemcasa.b_application.compras;

public class ResumoDesatualizadoException extends RuntimeException {
    private final PreviaConclusaoResponse resumoAtualizado;

    public ResumoDesatualizadoException(PreviaConclusaoResponse resumoAtualizado) {
        super("A lista ou o estoque mudou. Confira o resumo atualizado antes de concluir.");
        this.resumoAtualizado = resumoAtualizado;
    }

    public PreviaConclusaoResponse getResumoAtualizado() {
        return resumoAtualizado;
    }
}
