package com.activehub.usecases.cerrarreportesoporte;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/soporte/reportes")
public class CerrarReporteSoporteController {

    private final CerrarReporteSoporteService cerrarReporteSoporteService;

    public CerrarReporteSoporteController(CerrarReporteSoporteService cerrarReporteSoporteService) {
        this.cerrarReporteSoporteService = cerrarReporteSoporteService;
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("@permisos.puede('soporte.gestionar')")
    public CerrarReporteSoporteResponse cerrar(
            @PathVariable UUID id, @Valid @RequestBody CerrarReporteSoporteRequest request, Authentication authentication
    ) {
        UUID actorId = (UUID) authentication.getPrincipal();
        return cerrarReporteSoporteService.cerrar(id, request, actorId);
    }
}
