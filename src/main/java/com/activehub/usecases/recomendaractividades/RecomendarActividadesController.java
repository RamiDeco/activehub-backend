package com.activehub.usecases.recomendaractividades;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b>No es publico, y no puede serlo.</b> Una recomendacion es sobre una persona concreta: el
 * alumno sale del token y nunca de un parametro, asi que nadie puede pedir las de otro. La
 * landing publica sigue mostrando el catalogo y las mejor calificadas, sin pasar por aca.
 *
 * <p>{@code lat} y {@code lng} son opcionales porque la geolocalizacion del navegador es a
 * demanda y puede estar denegada: sin ellas la cercania no participa del puntaje, en vez de
 * inventar una distancia.
 */
@RestController
@RequestMapping("/api/alumno/recomendaciones")
public class RecomendarActividadesController {

    private final RecomendarActividadesService recomendarActividadesService;

    public RecomendarActividadesController(RecomendarActividadesService recomendarActividadesService) {
        this.recomendarActividadesService = recomendarActividadesService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('catalogo.explorar')")
    public RecomendarActividadesResponse recomendar(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer limite,
            Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return recomendarActividadesService.recomendar(alumnoId, lat, lng, limite);
    }
}
