package com.activehub.shared.security;

import com.activehub.shared.error.ApiError;
import com.activehub.shared.error.ApiErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Un instructor con una multa sin pagar (V36) no modifica nada de su oferta.
 *
 * <p>Decision del usuario: al aplicarse la multa queda bloqueado y solo ve la pantalla donde la
 * paga. Lo unico que se le deja hacer, ademas, es ver los inscriptos de sus clases y confirmar
 * cobros en efectivo: si no, el bloqueo lo pagarian los alumnos que ya le pagaron en mano.
 *
 * <p><b>Por que un filtro y no una guarda en cada slice.</b> Son unas quince operaciones del
 * instructor y la regla es la misma para todas: cualquier POST, PUT, PATCH o DELETE bajo
 * {@code /api/instructor/}. Escrita slice por slice, la proxima operacion que se agregue se la
 * olvida. Las seis que crean, editan o eliminan actividades y clases ademas lo vuelven a
 * preguntar en el Service ({@link PenalizacionVigenteGuard#exigirPuedeOperar}), junto con la
 * suspension. Las lecturas pasan: la pantalla de pago y la lista de inscriptos las necesitan.
 *
 * <p>Va despues del filtro de autenticacion: necesita saber quien llama. Sin sesion no hay nada
 * que restringir; de eso se encarga la cadena de seguridad.
 */
public class MultaPendienteFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_QUE_MODIFICAN = Set.of("POST", "PUT", "PATCH", "DELETE");

    /** Lo que un instructor con la multa sin pagar si puede hacer. */
    private static final List<String> PERMITIDAS = List.of(
            // Cargar el comprobante para pagar la multa.
            "/api/instructor/sanciones/**",
            // Confirmar un cobro en efectivo: protege al alumno que ya le pago en mano.
            "/api/instructor/inscripciones/*/confirmar-cobro",
            // Lo llama Mercado Pago, no el instructor.
            "/api/instructor/mercadopago/webhook");

    private final PenalizacionVigenteGuard penalizacionVigenteGuard;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher rutas = new AntPathMatcher();

    public MultaPendienteFilter(PenalizacionVigenteGuard penalizacionVigenteGuard, ObjectMapper objectMapper) {
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !METODOS_QUE_MODIFICAN.contains(request.getMethod())
                || !uri.startsWith("/api/instructor/")
                || PERMITIDAS.stream().anyMatch(patron -> rutas.match(patron, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID usuarioId
                && penalizacionVigenteGuard.tieneMultaPendiente(usuarioId)) {
            responderBloqueado(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Mismo shape de error que {@code GlobalExceptionHandler}, como {@link EmailVerificadoFilter}. */
    private void responderBloqueado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.SIN_PERMISO.getStatus().value(),
                ApiErrorCode.SIN_PERMISO.name(),
                "Tenés una multa pendiente de pago. Cargá el comprobante desde \"Pago de penalización\": "
                        + "vas a poder operar cuando la administración lo apruebe.",
                null,
                request.getRequestURI());
        response.setStatus(ApiErrorCode.SIN_PERMISO.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
