package com.activehub.usecases.listarresenasactividad;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actividades")
public class ListarResenasActividadController {

    private final ListarResenasActividadService listarResenasActividadService;

    public ListarResenasActividadController(ListarResenasActividadService listarResenasActividadService) {
        this.listarResenasActividadService = listarResenasActividadService;
    }

    @GetMapping("/{id}/resenas")
    public List<ListarResenasActividadResponse> listar(@PathVariable UUID id) {
        return listarResenasActividadService.listar(id);
    }
}
