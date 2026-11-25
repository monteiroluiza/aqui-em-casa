package io.github.monteiroluiza.aquiemcasa.d_adapter.web;

import io.github.monteiroluiza.aquiemcasa.b_application.compras.ResumoDesatualizadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CompraExceptionHandler {
    @ExceptionHandler(ResumoDesatualizadoException.class)
    ProblemDetail resumoDesatualizado(ResumoDesatualizadoException erro) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
        problema.setTitle("Resumo desatualizado");
        problema.setProperty("resumoAtualizado", erro.getResumoAtualizado());
        return problema;
    }
}
