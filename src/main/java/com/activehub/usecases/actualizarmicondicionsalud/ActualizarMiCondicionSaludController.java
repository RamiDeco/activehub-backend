package com.activehub.usecases.actualizarmicondicionsalud;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sin {@code @PreAuthorize}, igual que {@code actualizarmisintereses}: es un dato de la propia
 * cuenta, no un módulo. El Service resuelve el perfil de alumno de quien llama.
 */
@RestController
@RequestMapping("/api/usuarios/me/salud")
public class ActualizarMiCondicionSaludController {

    private final ActualizarMiCondicionSaludService actualizarMiCondicionSaludService;

    public ActualizarMiCondicionSaludController(ActualizarMiCondicionSaludService actualizarMiCondicionSaludService) {
        this.actualizarMiCondicionSaludService = actualizarMiCondicionSaludService;
    }

    @PutMapping
    public ActualizarMiCondicionSaludResponse actualizar(
            @Valid @RequestBody ActualizarMiCondicionSaludRequest request, Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return actualizarMiCondicionSaludService.actualizar(usuarioId, request);
    }
}
