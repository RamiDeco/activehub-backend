package com.activehub.usecases.quitarfavorito;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/actividades/{actividadId}/favorito")
public class QuitarFavoritoController {

    private final QuitarFavoritoService quitarFavoritoService;

    public QuitarFavoritoController(QuitarFavoritoService quitarFavoritoService) {
        this.quitarFavoritoService = quitarFavoritoService;
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<Void> quitar(@PathVariable UUID actividadId, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        quitarFavoritoService.quitar(actividadId, alumnoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
