package com.activehub.usecases.listarclasesinstructor;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class ListarClasesInstructorController {

    private final ListarClasesInstructorService listarClasesInstructorService;

    public ListarClasesInstructorController(ListarClasesInstructorService listarClasesInstructorService) {
        this.listarClasesInstructorService = listarClasesInstructorService;
    }

    @GetMapping("/{id}/clases")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListarClasesInstructorResponse> listar(@PathVariable UUID id) {
        return listarClasesInstructorService.listar(id);
    }
}
