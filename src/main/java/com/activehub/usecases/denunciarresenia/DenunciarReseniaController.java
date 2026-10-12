package com.activehub.usecases.denunciarresenia;

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
@RequestMapping("/api/instructor/resenas")
public class DenunciarReseniaController {

    private final DenunciarReseniaService denunciarReseniaService;

    public DenunciarReseniaController(DenunciarReseniaService denunciarReseniaService) {
        this.denunciarReseniaService = denunciarReseniaService;
    }

    @PostMapping("/{id}/denuncia")
    @PreAuthorize("@permisos.puede('resenias.responder')")
    public ResponseEntity<DenunciarReseniaResponse> denunciar(
            @PathVariable UUID id, @Valid @RequestBody DenunciarReseniaRequest request, Authentication authentication
    ) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        DenunciarReseniaResponse response = denunciarReseniaService.denunciar(id, request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
