package com.activehub.usecases.listarinstructores;

import com.activehub.domain.usuario.EstadoVerificacion;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class ListarInstructoresController {

    private final ListarInstructoresService listarInstructoresService;

    public ListarInstructoresController(ListarInstructoresService listarInstructoresService) {
        this.listarInstructoresService = listarInstructoresService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('instructores.validar')")
    public List<ListarInstructoresResponse> listar(
            @RequestParam(value = "estado", required = false) EstadoVerificacion estadoVerificacion) {
        return listarInstructoresService.listar(estadoVerificacion);
    }
}
