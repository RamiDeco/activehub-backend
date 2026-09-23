package com.activehub.usecases.generarinformeactividad;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b>No puede ser público, y no puede tomar el alumno de un parámetro.</b> El informe habla del
 * perfil de una persona concreta —sus intereses, su edad, su condición de salud, lo que hizo en la
 * plataforma—, así que el alumno sale del token y de ningún otro lado. Es el mismo criterio que
 * {@code RecomendarActividadesController}.
 *
 * <p>Es POST aunque no escriba nada en la base: genera un recurso nuevo cada vez, consume cuota de un
 * servicio externo y no es cacheable ni repetible sin costo. Un GET con esas propiedades lo
 * reintentaría cualquier proxy.
 *
 * <p>El permiso es {@code catalogo.explorar}, el mismo que gatea las recomendaciones y los favoritos:
 * es la misma clase de acción, un alumno mirando el catálogo.
 */
@RestController
@RequestMapping("/api/alumno/actividades/{actividadId}/informe")
public class GenerarInformeActividadController {

    private final GenerarInformeActividadService generarInformeActividadService;

    public GenerarInformeActividadController(
            GenerarInformeActividadService generarInformeActividadService) {
        this.generarInformeActividadService = generarInformeActividadService;
    }

    @PostMapping
    @PreAuthorize("@permisos.puede('catalogo.explorar')")
    public GenerarInformeActividadResponse generar(
            @PathVariable UUID actividadId,
            @RequestBody(required = false) GenerarInformeActividadRequest request,
            Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return generarInformeActividadService.generar(actividadId, alumnoId, request);
    }
}
