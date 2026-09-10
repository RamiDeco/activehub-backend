package com.activehub.usecases.obtenermiperfilinstructor;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/perfil")
public class ObtenerMiPerfilInstructorController {

    private final ObtenerMiPerfilInstructorService obtenerMiPerfilInstructorService;

    public ObtenerMiPerfilInstructorController(ObtenerMiPerfilInstructorService obtenerMiPerfilInstructorService) {
        this.obtenerMiPerfilInstructorService = obtenerMiPerfilInstructorService;
    }

    @GetMapping
    // RN-16: es "Mis datos", no un módulo con permiso. El servicio resuelve el
    // PerfilInstructor del que llama; quien no tenga uno recibe 404, no importa su rol.
    public ObtenerMiPerfilInstructorResponse obtener(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return obtenerMiPerfilInstructorService.obtener(usuarioId);
    }
}
