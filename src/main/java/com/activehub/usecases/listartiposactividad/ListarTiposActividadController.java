package com.activehub.usecases.listartiposactividad;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tipos-actividad")
public class ListarTiposActividadController {

    private final ListarTiposActividadService listarTiposActividadService;

    public ListarTiposActividadController(ListarTiposActividadService listarTiposActividadService) {
        this.listarTiposActividadService = listarTiposActividadService;
    }

    @GetMapping
    public List<ListarTiposActividadResponse> listar(@RequestParam(required = false) UUID categoriaId) {
        return listarTiposActividadService.listar(categoriaId);
    }
}
