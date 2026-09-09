package com.activehub.usecases.listarpenalizaciones;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/penalizaciones")
public class ListarPenalizacionesController {

    private final ListarPenalizacionesService listarPenalizacionesService;

    public ListarPenalizacionesController(ListarPenalizacionesService listarPenalizacionesService) {
        this.listarPenalizacionesService = listarPenalizacionesService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListarPenalizacionesResponse> listar() {
        return listarPenalizacionesService.listar();
    }
}
