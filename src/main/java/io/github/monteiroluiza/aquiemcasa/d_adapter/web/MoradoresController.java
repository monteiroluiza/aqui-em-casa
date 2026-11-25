package io.github.monteiroluiza.aquiemcasa.d_adapter.web;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import io.github.monteiroluiza.aquiemcasa.a_domain.Papel;
import io.github.monteiroluiza.aquiemcasa.b_application.moradores.MoradoresService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController
public class MoradoresController {
    private final MoradoresService service;
    public MoradoresController(MoradoresService service) { this.service=service; }
    public record ConviteRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record PapelRequest(@NotNull Papel papel) {}
    public record AceitarRequest(@NotBlank @Size(max=100) String codigo, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=128) String senha, @Size(max=150) String nome) {}
    @GetMapping("/api/casas/{casaId}/moradores") @SecurityRequirement(name="bearer")
    public List<Map<String,Object>> listar(@PathVariable UUID casaId,Principal p) { return service.listar(casaId,p.getName()); }
    @PostMapping("/api/casas/{casaId}/convites") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="bearer")
    public Map<String,Object> convidar(@PathVariable UUID casaId,Principal p,@Valid @RequestBody ConviteRequest r) { return service.convidar(casaId,p.getName(),r.email()); }
    @GetMapping("/api/casas/{casaId}/convites") @SecurityRequirement(name="bearer")
    public List<Map<String,Object>> convites(@PathVariable UUID casaId,Principal p) { return service.convites(casaId,p.getName()); }
    @DeleteMapping("/api/casas/{casaId}/convites/{conviteId}") @ResponseStatus(HttpStatus.NO_CONTENT) @SecurityRequirement(name="bearer")
    public void cancelar(@PathVariable UUID casaId,@PathVariable UUID conviteId,Principal p) { service.cancelar(casaId,conviteId,p.getName()); }
    @PostMapping("/api/convites/aceitar")
    public Map<String,Object> aceitar(@Valid @RequestBody AceitarRequest r) { return service.aceitar(r.codigo(),r.email(),r.senha(),r.nome()); }
    @PatchMapping("/api/casas/{casaId}/moradores/{membroId}/papel") @ResponseStatus(HttpStatus.NO_CONTENT) @SecurityRequirement(name="bearer")
    public void papel(@PathVariable UUID casaId,@PathVariable UUID membroId,Principal p,@Valid @RequestBody PapelRequest r) { service.alterar(casaId,membroId,p.getName(),r.papel(),false); }
    @DeleteMapping("/api/casas/{casaId}/moradores/{membroId}") @ResponseStatus(HttpStatus.NO_CONTENT) @SecurityRequirement(name="bearer")
    public void remover(@PathVariable UUID casaId,@PathVariable UUID membroId,Principal p) { service.alterar(casaId,membroId,p.getName(),null,true); }
}
