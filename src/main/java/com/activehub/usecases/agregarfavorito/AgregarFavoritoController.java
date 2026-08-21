package com.activehub.usecases.agregarfavorito;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/actividades/{actividadId}/favorito")
public class AgregarFavoritoController {

    private final AgregarFavoritoService agregarFavoritoService;

    public AgregarFavoritoController(AgregarFavoritoService agregarFavoritoService) {
        this.agregarFavoritoService = agregarFavoritoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<Void> agregar(@PathVariable UUID actividadId, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        agregarFavoritoService.agregar(actividadId, alumnoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
