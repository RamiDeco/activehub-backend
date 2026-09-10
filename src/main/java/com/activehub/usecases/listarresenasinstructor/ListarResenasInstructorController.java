package com.activehub.usecases.listarresenasinstructor;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/resenas")
public class ListarResenasInstructorController {

    private final ListarResenasInstructorService listarResenasInstructorService;

    public ListarResenasInstructorController(ListarResenasInstructorService listarResenasInstructorService) {
        this.listarResenasInstructorService = listarResenasInstructorService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('resenias.responder')")
    public List<ListarResenasInstructorResponse> listar(Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return listarResenasInstructorService.listar(instructorId);
    }
}
