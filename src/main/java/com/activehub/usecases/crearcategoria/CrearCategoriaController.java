package com.activehub.usecases.crearcategoria;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categorias")
public class CrearCategoriaController {

    private final CrearCategoriaService crearCategoriaService;

    public CrearCategoriaController(CrearCategoriaService crearCategoriaService) {
        this.crearCategoriaService = crearCategoriaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CrearCategoriaResponse> crear(
            @Valid @RequestBody CrearCategoriaRequest request, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        CrearCategoriaResponse response = crearCategoriaService.crear(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
