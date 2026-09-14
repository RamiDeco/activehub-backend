package com.activehub.usecases.listarinscripcionesadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inscripciones")
public class ListarInscripcionesAdminController {

    private final ListarInscripcionesAdminService listarInscripcionesAdminService;

    public ListarInscripcionesAdminController(ListarInscripcionesAdminService listarInscripcionesAdminService) {
        this.listarInscripcionesAdminService = listarInscripcionesAdminService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('reportes.ver')")
    public List<ListarInscripcionesAdminResponse> listar() {
        return listarInscripcionesAdminService.listar();
    }
}
