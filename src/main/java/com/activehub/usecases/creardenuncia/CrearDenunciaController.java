package com.activehub.usecases.creardenuncia;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/clases")
public class CrearDenunciaController {

    private final CrearDenunciaService crearDenunciaService;

    public CrearDenunciaController(CrearDenunciaService crearDenunciaService) {
        this.crearDenunciaService = crearDenunciaService;
    }

    @PostMapping("/{claseId}/denuncias")
    @PreAuthorize("@permisos.puede('denuncias.crear')")
    public ResponseEntity<CrearDenunciaResponse> crear(
            @PathVariable UUID claseId, @Valid @RequestBody CrearDenunciaRequest request, Authentication authentication
    ) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        CrearDenunciaResponse response = crearDenunciaService.crear(claseId, request, alumnoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
