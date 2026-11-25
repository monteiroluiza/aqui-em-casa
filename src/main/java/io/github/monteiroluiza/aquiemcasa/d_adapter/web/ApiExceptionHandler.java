package io.github.monteiroluiza.aquiemcasa.d_adapter.web;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail negocio(ResponseStatusException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason()==null ? "Nao foi possivel concluir a operacao." : e.getReason());
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var p=ProblemDetail.forStatusAndDetail(status,"Revise os campos informados.");
        p.setProperty("erros",e.getBindingResult().getFieldErrors().stream()
            .map(f -> Map.of("campo",f.getField(),"mensagem",Objects.toString(f.getDefaultMessage(),"Valor invalido."))).toList());
        return handleExceptionInternal(e,p,headers,status,request);
    }
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e,Object body,HttpHeaders headers,
            HttpStatusCode status,WebRequest request) {
        if(body==null || body instanceof ProblemDetail) {
            var p=body instanceof ProblemDetail detalhe ? detalhe : ProblemDetail.forStatus(status);
            if(!(e instanceof MethodArgumentNotValidException)) p.setDetail(switch(status.value()) {
                case 400 -> "Requisicao invalida. Confira o JSON, os parametros e os tipos dos campos.";
                case 404 -> "Recurso nao encontrado.";
                case 405 -> "Metodo HTTP nao permitido para este recurso.";
                case 415 -> "Formato de conteudo nao suportado. Utilize application/json.";
                default -> "Nao foi possivel processar a requisicao.";
            });
            body=p;
        }
        return super.handleExceptionInternal(e,body,headers,status,request);
    }
    @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class})
    ProblemDetail conflito(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Os dados conflitam com outro registro ou foram alterados. Atualize a consulta e tente novamente."); }
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail proibido(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,"Voce nao tem permissao para esta operacao."); }
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail autenticacao(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,"Credenciais invalidas ou autenticacao expirada."); }
    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        logger.error("Falha inesperada ao processar requisicao",e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"Nao foi possivel concluir a operacao. Tente novamente mais tarde.");
    }
}
