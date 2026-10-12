package com.activehub.usecases.registraradmin;

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
@RequestMapping("/api/admin/usuarios/admin")
public class RegistrarAdminController {

    private final RegistrarAdminService registrarAdminService;

    public RegistrarAdminController(RegistrarAdminService registrarAdminService) {
        this.registrarAdminService = registrarAdminService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('usuarios.gestionar')")
    public ResponseEntity<RegistrarAdminResponse> registrar(
            @Valid @RequestBody RegistrarAdminRequest request, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        RegistrarAdminResponse response = registrarAdminService.registrar(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
