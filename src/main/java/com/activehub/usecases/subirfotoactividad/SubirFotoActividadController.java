package com.activehub.usecases.subirfotoactividad;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/instructor/actividades")
public class SubirFotoActividadController {

    private final SubirFotoActividadService subirFotoActividadService;

    public SubirFotoActividadController(SubirFotoActividadService subirFotoActividadService) {
        this.subirFotoActividadService = subirFotoActividadService;
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ResponseEntity<SubirFotoActividadResponse> subir(
            @PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        boolean esAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        SubirFotoActividadResponse response = subirFotoActividadService.subir(id, archivo, actorId, esAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
