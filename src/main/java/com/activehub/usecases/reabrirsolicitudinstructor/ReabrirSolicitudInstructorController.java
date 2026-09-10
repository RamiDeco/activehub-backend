package com.activehub.usecases.reabrirsolicitudinstructor;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/solicitud")
public class ReabrirSolicitudInstructorController {

    private final ReabrirSolicitudInstructorService reabrirSolicitudInstructorService;

    public ReabrirSolicitudInstructorController(
            ReabrirSolicitudInstructorService reabrirSolicitudInstructorService) {
        this.reabrirSolicitudInstructorService = reabrirSolicitudInstructorService;
    }

    /** No lleva la guarda de instructor verificado: es justamente para el que NO lo está. */
    @PostMapping("/reabrir")
    // RN-16: es 'Mis datos', no un módulo con permiso. El servicio resuelve el
    // PerfilInstructor del que llama; quien no tenga uno recibe 404, no importa su rol.
    public ReabrirSolicitudInstructorResponse reabrir(Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return reabrirSolicitudInstructorService.reabrir(instructorId);
    }
}
