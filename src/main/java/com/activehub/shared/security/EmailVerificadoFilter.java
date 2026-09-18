package com.activehub.shared.security;

import com.activehub.shared.error.ApiError;
import com.activehub.shared.error.ApiErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Una cuenta sin el correo confirmado <b>no puede hacer nada</b> más que confirmarlo.
 *
 * <h2>Por qué esto vive en el backend y no sólo en las rutas del frontend</h2>
 *
 * La guarda de rutas de React evita que el usuario <i>navegue</i>, pero no evita que alguien
 * use el token que recibió al registrarse para llamar a la API directamente. Sin este filtro,
 * "no podés hacer nada hasta verificar" sería una afirmación sobre la pantalla, no sobre el
 * sistema: bastaría un fetch para inscribirse sin haber confirmado nunca el correo.
 *
 * <h2>El flag sale del token, no de la base</h2>
 *
 * Ir a buscar el usuario en cada request para leer un booleano es un viaje a la base por cada
 * llamada de la aplicación. El claim alcanza porque **sólo puede pasar de false a true**: un
 * token viejo nunca habilita de más, y {@code verificaremail} devuelve un token nuevo, así que
 * la restricción se levanta en el acto en vez de esperar a que venza el anterior.
 *
 * <p>Los tokens emitidos <b>antes</b> de que existiera el claim se tratan como verificados:
 * ausente no es false. Si no, al desplegar esto todas las sesiones abiertas quedarían
 * encerradas en la pantalla del código sin haber hecho nada.
 */
public class EmailVerificadoFilter extends OncePerRequestFilter {

    /**
     * Lo único que puede hacer una cuenta sin confirmar. Es deliberadamente mínimo:
     *
     * <ul>
     *   <li>los dos endpoints del código (confirmarlo y pedir uno nuevo);</li>
     *   <li>{@code /api/auth/me}, que es de donde el frontend saca la sesión para saber que
     *       tiene que mandarlo a la pantalla del código;</li>
     *   <li>{@code /api/auth/refresh}, para que la sesión no se muera mientras la persona va
     *       a buscar el mail.</li>
     * </ul>
     *
     * <p>El cambio de correo NO está: si alguien se equivocó al tipearlo, el camino es
     * registrarse de nuevo — su dirección errónea no quedó reservada, justamente porque no la
     * confirmó.
     */
    private static final Set<String> PERMITIDAS = Set.of(
            "/api/auth/verificar-email",
            "/api/auth/verificar-email/reenviar",
            "/api/auth/me",
            "/api/auth/refresh");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public EmailVerificadoFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // Sin token no hay nada que restringir: de eso se encarga la cadena de seguridad.
            filterChain.doFilter(request, response);
            return;
        }

        if (PERMITIDAS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.validarYObtenerClaims(header.substring("Bearer ".length()));
            // Ausente = token viejo, de antes del claim: se trata como verificado.
            Boolean verificado = claims.get("emailVerificado", Boolean.class);
            if (verificado != null && !verificado) {
                responderBloqueado(request, response);
                return;
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Token roto o vencido: no es asunto de este filtro. El de autenticación ya no
            // puso sesión en el contexto, así que la cadena responderá 401 sola.
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Mismo shape de error que {@code GlobalExceptionHandler}: el cliente no puede distinguir
     * si un error salió de un filtro o de un controller, y el frontend parsea uno solo.
     */
    private void responderBloqueado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.EMAIL_SIN_VERIFICAR.getStatus().value(),
                ApiErrorCode.EMAIL_SIN_VERIFICAR.name(),
                "Confirmá tu correo con el código que te enviamos para poder usar tu cuenta.",
                null,
                request.getRequestURI());
        response.setStatus(ApiErrorCode.EMAIL_SIN_VERIFICAR.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
