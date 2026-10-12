package com.activehub.usecases.listarrolespermisos;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/roles")
public class ListarRolesPermisosController {

    private final ListarRolesPermisosService listarRolesPermisosService;

    public ListarRolesPermisosController(ListarRolesPermisosService listarRolesPermisosService) {
        this.listarRolesPermisosService = listarRolesPermisosService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('roles.configurar')")
    public ListarRolesPermisosResponse listar() {
        return listarRolesPermisosService.listar();
    }
}
