package com.activehub.usecases.listarclasesadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/clases")
public class ListarClasesAdminController {

    private final ListarClasesAdminService listarClasesAdminService;

    public ListarClasesAdminController(ListarClasesAdminService listarClasesAdminService) {
        this.listarClasesAdminService = listarClasesAdminService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListarClasesAdminResponse> listar() {
        return listarClasesAdminService.listar();
    }
}
