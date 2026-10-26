package com.activehub.usecases.retirarmidocumento;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetirarMiDocumentoController {

    private final RetirarMiDocumentoService retirarMiDocumentoService;

    public RetirarMiDocumentoController(RetirarMiDocumentoService retirarMiDocumentoService) {
        this.retirarMiDocumentoService = retirarMiDocumentoService;
    }

    @DeleteMapping("/api/instructor/documentos/{documentoId}")
    // RN-16: "Mis datos", sin permiso; sólo los documentos propios.
    public ResponseEntity<Void> retirar(@PathVariable UUID documentoId, Authentication authentication) {
        retirarMiDocumentoService.retirar((UUID) authentication.getPrincipal(), documentoId);
        return ResponseEntity.noContent().build();
    }
}
