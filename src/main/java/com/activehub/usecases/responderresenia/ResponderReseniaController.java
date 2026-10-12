package com.activehub.usecases.responderresenia;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/resenas")
public class ResponderReseniaController {

    private final ResponderReseniaService responderReseniaService;

    public ResponderReseniaController(ResponderReseniaService responderReseniaService) {
        this.responderReseniaService = responderReseniaService;
    }

    @PostMapping("/{id}/respuesta")
    @PreAuthorize("@permisos.puede('resenias.responder')")
    public ResponderReseniaResponse responder(
            @PathVariable UUID id, @Valid @RequestBody ResponderReseniaRequest request, Authentication authentication
    ) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return responderReseniaService.responder(id, request, instructorId);
    }
}
