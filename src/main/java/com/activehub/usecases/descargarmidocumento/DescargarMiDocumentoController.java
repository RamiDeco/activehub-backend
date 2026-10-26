package com.activehub.usecases.descargarmidocumento;

import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DescargarMiDocumentoController {

    private final DescargarMiDocumentoService descargarMiDocumentoService;

    public DescargarMiDocumentoController(DescargarMiDocumentoService descargarMiDocumentoService) {
        this.descargarMiDocumentoService = descargarMiDocumentoService;
    }

    @GetMapping("/api/instructor/documentos/{documentoId}/archivo")
    // RN-16: "Mis datos", sin permiso; sólo los documentos propios.
    public ResponseEntity<byte[]> descargar(@PathVariable UUID documentoId, Authentication authentication) {
        DescargarMiDocumentoResponse documento =
                descargarMiDocumentoService.descargar((UUID) authentication.getPrincipal(), documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documento.nombreArchivo() + "\"")
                .body(documento.contenido());
    }
}
