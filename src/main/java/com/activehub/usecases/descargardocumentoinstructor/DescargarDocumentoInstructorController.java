package com.activehub.usecases.descargardocumentoinstructor;

import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class DescargarDocumentoInstructorController {

    private final DescargarDocumentoInstructorService descargarDocumentoInstructorService;

    public DescargarDocumentoInstructorController(
            DescargarDocumentoInstructorService descargarDocumentoInstructorService) {
        this.descargarDocumentoInstructorService = descargarDocumentoInstructorService;
    }

    @GetMapping("/{instructorId}/documentos/{documentoId}/archivo")
    @PreAuthorize("@permisos.puede('instructores.validar')")
    public ResponseEntity<byte[]> descargar(
            @PathVariable UUID instructorId, @PathVariable UUID documentoId) {
        DocumentoDescarga documento = descargarDocumentoInstructorService.descargar(instructorId, documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documento.nombreArchivo() + "\"")
                .body(documento.contenido());
    }
}
