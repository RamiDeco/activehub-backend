package com.activehub.usecases.listarusuariosadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/usuarios")
public class ListarUsuariosAdminController {

    private final ListarUsuariosAdminService listarUsuariosAdminService;

    public ListarUsuariosAdminController(ListarUsuariosAdminService listarUsuariosAdminService) {
        this.listarUsuariosAdminService = listarUsuariosAdminService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('usuarios.gestionar')")
    public List<ListarUsuariosAdminResponse> listar() {
        return listarUsuariosAdminService.listar();
    }
}
