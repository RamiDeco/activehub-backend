package com.activehub.usecases.listaractividades;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actividades")
public class ListarActividadesController {

    private final ListarActividadesService listarActividadesService;

    public ListarActividadesController(ListarActividadesService listarActividadesService) {
        this.listarActividadesService = listarActividadesService;
    }

    @GetMapping
    public List<ListarActividadesResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID tipoActividadId,
            @RequestParam(required = false) String nivelIntensidad,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false, defaultValue = "false") boolean soloConCupos,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) String sort
    ) {
        return listarActividadesService.listar(
                texto, categoriaId, tipoActividadId, nivelIntensidad, precioMax, soloConCupos, instructorId, sort);
    }
}
