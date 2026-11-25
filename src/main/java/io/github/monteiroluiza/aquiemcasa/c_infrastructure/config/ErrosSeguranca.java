package io.github.monteiroluiza.aquiemcasa.c_infrastructure.config;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
public final class ErrosSeguranca {
    private ErrosSeguranca() {}
    public static void responder(HttpServletResponse response,int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        if(status==401) response.setHeader("WWW-Authenticate","Bearer");
        String title=status==401 ? "Unauthorized" : "Forbidden";
        String detail=status==401 ? "Informe um Bearer Token valido e nao expirado." : "Voce nao tem permissao para esta operacao.";
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\""+title+"\",\"status\":"+status+",\"detail\":\""+detail+"\"}");
    }
}
