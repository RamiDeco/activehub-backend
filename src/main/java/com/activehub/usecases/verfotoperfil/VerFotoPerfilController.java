package com.activehub.usecases.verfotoperfil;

import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/fotos/perfil")
public class VerFotoPerfilController {

    private final VerFotoPerfilService verFotoPerfilService;

    public VerFotoPerfilController(VerFotoPerfilService verFotoPerfilService) {
        this.verFotoPerfilService = verFotoPerfilService;
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<byte[]> ver(@PathVariable UUID usuarioId) {
        FotoDescarga foto = verFotoPerfilService.ver(usuarioId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(foto.contenido());
    }
}
