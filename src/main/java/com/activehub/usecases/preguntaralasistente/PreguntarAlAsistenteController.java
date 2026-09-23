package com.activehub.usecases.preguntaralasistente;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint <b>público</b> (ver la lista {@code permitAll} de {@code SecurityConfig}), por el mismo
 * motivo que "Reportar un problema": la burbuja "¿Dudas?" no vive solo en las pantallas del alumno,
 * también está en la pantalla de Ayuda, que se usa sin sesión. Exigir token dejaría sin asistente a
 * quien justamente no puede entrar.
 *
 * <p>Público no es libre: {@code LimiteConsultasIa} cuenta las consultas por usuario o por IP, para
 * que nadie agote solo la cuota gratuita del modelo. La IP se resuelve acá y no en el service porque
 * es un detalle del transporte.
 *
 * <p>No lleva {@code @PreAuthorize} a propósito: no hay un permiso del catálogo que lo gatee, igual
 * que el resto de los endpoints públicos (el test que cruza guardas contra permisos solo exige que
 * toda clave del catálogo gatee algo, no que todo endpoint tenga clave).
 */
@RestController
@RequestMapping("/api/asistente/consultas")
public class PreguntarAlAsistenteController {

    private final PreguntarAlAsistenteService preguntarAlAsistenteService;

    public PreguntarAlAsistenteController(PreguntarAlAsistenteService preguntarAlAsistenteService) {
        this.preguntarAlAsistenteService = preguntarAlAsistenteService;
    }

    @PostMapping
    public PreguntarAlAsistenteResponse preguntar(
            @Valid @RequestBody PreguntarAlAsistenteRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String clave = authentication != null
                ? "usuario:" + (UUID) authentication.getPrincipal()
                : "ip:" + ipDe(http);
        return preguntarAlAsistenteService.responder(request, clave);
    }

    /**
     * La IP del cliente. Se mira {@code X-Forwarded-For} porque el despliegue va detrás de un proxy
     * (Render/Vercel) y ahí {@code getRemoteAddr()} devuelve la del proxy — o sea, una sola clave
     * para todo el mundo, que convertiría el límite por cliente en un límite global.
     *
     * <p>El header lo puede falsificar quien llame directo al backend, así que esto no sirve como
     * identificación ni como control de acceso; solo reparte la cuota. Para eso, que alguien se
     * reparta a sí mismo en varias claves es un problema menor comparado con dejar a todos los
     * usuarios legítimos compartiendo una.
     */
    private String ipDe(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Puede venir con varios saltos: "cliente, proxy1, proxy2". El primero es el cliente.
            return forwarded.split(",")[0].strip();
        }
        String remota = http.getRemoteAddr();
        return remota == null ? "desconocida" : remota;
    }
}
