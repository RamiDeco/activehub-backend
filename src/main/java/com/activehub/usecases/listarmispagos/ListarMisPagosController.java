package com.activehub.usecases.listarmispagos;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/pagos")
public class ListarMisPagosController {

    private final ListarMisPagosService listarMisPagosService;

    public ListarMisPagosController(ListarMisPagosService listarMisPagosService) {
        this.listarMisPagosService = listarMisPagosService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('inscripciones.gestionar')")
    public List<ListarMisPagosResponse> listar(Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return listarMisPagosService.listar(alumnoId);
    }
}
