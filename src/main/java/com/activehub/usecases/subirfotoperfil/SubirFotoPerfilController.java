package com.activehub.usecases.subirfotoperfil;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/usuarios/foto")
public class SubirFotoPerfilController {

    private final SubirFotoPerfilService subirFotoPerfilService;

    public SubirFotoPerfilController(SubirFotoPerfilService subirFotoPerfilService) {
        this.subirFotoPerfilService = subirFotoPerfilService;
    }

    @PostMapping
    public ResponseEntity<SubirFotoPerfilResponse> subir(
            @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        SubirFotoPerfilResponse response = subirFotoPerfilService.subir(archivo, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
