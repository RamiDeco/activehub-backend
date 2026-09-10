package com.activehub.usecases.subirdocumento;

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
@RequestMapping("/api/instructor/documentos")
public class SubirDocumentoController {

    private final SubirDocumentoService subirDocumentoService;

    public SubirDocumentoController(SubirDocumentoService subirDocumentoService) {
        this.subirDocumentoService = subirDocumentoService;
    }

    @PostMapping
    // RN-16: es "Mis datos", no un módulo con permiso. El servicio resuelve el
    // PerfilInstructor del que llama; quien no tenga uno recibe 404, no importa su rol.
    public ResponseEntity<SubirDocumentoResponse> subir(
            @RequestParam("archivo") MultipartFile archivo, Authentication authentication) {
        UUID actorId = (UUID) authentication.getPrincipal();
        SubirDocumentoResponse response = subirDocumentoService.subir(archivo, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
