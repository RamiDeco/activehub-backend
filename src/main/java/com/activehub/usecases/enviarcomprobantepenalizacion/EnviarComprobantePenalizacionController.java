package com.activehub.usecases.enviarcomprobantepenalizacion;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/instructor/sanciones")
public class EnviarComprobantePenalizacionController {

    private final EnviarComprobantePenalizacionService enviarComprobantePenalizacionService;

    public EnviarComprobantePenalizacionController(
            EnviarComprobantePenalizacionService enviarComprobantePenalizacionService) {
        this.enviarComprobantePenalizacionService = enviarComprobantePenalizacionService;
    }

    @PostMapping("/{id}/comprobante")
    // Sin permiso: es la multa propia (el servicio responde 404 a una ajena). Es lo unico,
    // junto con confirmar cobros, que MultaPendienteFilter deja pasar con la multa sin pagar.
    public ResponseEntity<EnviarComprobantePenalizacionResponse> enviar(
            @PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enviarComprobantePenalizacionService.enviar(id, archivo, actorId));
    }
}
