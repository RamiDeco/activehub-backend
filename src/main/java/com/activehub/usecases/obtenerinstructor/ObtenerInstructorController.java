package com.activehub.usecases.obtenerinstructor;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/instructores")
public class ObtenerInstructorController {

    private final ObtenerInstructorService obtenerInstructorService;

    public ObtenerInstructorController(ObtenerInstructorService obtenerInstructorService) {
        this.obtenerInstructorService = obtenerInstructorService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ObtenerInstructorResponse obtener(@PathVariable UUID id) {
        return obtenerInstructorService.obtener(id);
    }
}
