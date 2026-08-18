package com.activehub.usecases.listardenunciasadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/denuncias")
public class ListarDenunciasAdminController {

    private final ListarDenunciasAdminService listarDenunciasAdminService;

    public ListarDenunciasAdminController(ListarDenunciasAdminService listarDenunciasAdminService) {
        this.listarDenunciasAdminService = listarDenunciasAdminService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListarDenunciasAdminResponse> listar() {
        return listarDenunciasAdminService.listar();
    }
}
