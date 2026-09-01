package com.activehub.usecases.listardocumentosinstructor;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class ListarDocumentosInstructorController {

    private final ListarDocumentosInstructorService listarDocumentosInstructorService;

    public ListarDocumentosInstructorController(ListarDocumentosInstructorService listarDocumentosInstructorService) {
        this.listarDocumentosInstructorService = listarDocumentosInstructorService;
    }

    @GetMapping("/{id}/documentos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListarDocumentosInstructorResponse> listar(@PathVariable UUID id) {
        return listarDocumentosInstructorService.listar(id);
    }
}
