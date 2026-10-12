package com.activehub.usecases.registrarinteraccion;

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

/**
 * El alumno solo puede registrar señales <b>propias</b>: el actor sale del token, nunca del
 * cuerpo del pedido. Sin eso, cualquiera podria ensuciar las recomendaciones de otro.
 */
@RestController
@RequestMapping("/api/alumno/interacciones")
public class RegistrarInteraccionController {

    private final RegistrarInteraccionService registrarInteraccionService;

    public RegistrarInteraccionController(RegistrarInteraccionService registrarInteraccionService) {
        this.registrarInteraccionService = registrarInteraccionService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('catalogo.explorar')")
    public ResponseEntity<Void> registrar(
            @Valid @RequestBody RegistrarInteraccionRequest request, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        registrarInteraccionService.registrar(alumnoId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
