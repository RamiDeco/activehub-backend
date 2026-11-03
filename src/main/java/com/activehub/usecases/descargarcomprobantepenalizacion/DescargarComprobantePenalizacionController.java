package com.activehub.usecases.descargarcomprobantepenalizacion;

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
@RequestMapping("/api/admin/penalizaciones")
public class DescargarComprobantePenalizacionController {

    private final DescargarComprobantePenalizacionService descargarComprobantePenalizacionService;

    public DescargarComprobantePenalizacionController(
            DescargarComprobantePenalizacionService descargarComprobantePenalizacionService) {
        this.descargarComprobantePenalizacionService = descargarComprobantePenalizacionService;
    }

    @GetMapping("/comprobantes/{comprobanteId}/archivo")
    @PreAuthorize("@permisos.puede('penalizaciones.gestionar')")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID comprobanteId) {
        ComprobanteDescarga comprobante = descargarComprobantePenalizacionService.descargar(comprobanteId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(comprobante.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + comprobante.nombreArchivo() + "\"")
                .body(comprobante.contenido());
    }
}
