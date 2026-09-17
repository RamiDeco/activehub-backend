package com.activehub.usecases.listarreportessoporte;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/soporte/reportes")
public class ListarReportesSoporteController {

    private final ListarReportesSoporteService listarReportesSoporteService;

    public ListarReportesSoporteController(ListarReportesSoporteService listarReportesSoporteService) {
        this.listarReportesSoporteService = listarReportesSoporteService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('soporte.gestionar')")
    public List<ListarReportesSoporteResponse> listar() {
        return listarReportesSoporteService.listar();
    }
}
