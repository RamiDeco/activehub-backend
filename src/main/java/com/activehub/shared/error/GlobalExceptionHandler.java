package com.activehub.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import org.springframework.beans.factory.ObjectProvider;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Por ObjectProvider y no por constructor directo: este advice se importa en ~50
     * {@code @WebMvcTest} de slice, que levantan un contexto minimo sin AuditService. Exigirlo
     * como dependencia obligatoria haria fallar todos esos tests por un bean que no tiene nada
     * que ver con lo que prueban. Si no esta, no se audita y el 403 se devuelve igual.
     */
    private final ObjectProvider<AuditService> auditService;

    public GlobalExceptionHandler(ObjectProvider<AuditService> auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(),
                ex.getCode().getStatus().value(),
                ex.getCode().name(),
                ex.getMessage(),
                ex.getFieldErrors(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getCode().getStatus()).body(body);
    }

    /**
     * <b>Un intento de acceso denegado tambien se audita</b> (RN-14): que alguien pida algo para
     * lo que no tiene permiso es informacion de seguridad, no ruido. Un alumno pegandole a
     * /api/admin/roles es exactamente lo que una auditoria quiere poder ver, y antes no dejaba
     * rastro en ningun lado — el 403 se devolvia y ahi moria.
     *
     * <p>Se registra aca y no en un filtro porque {@code @PreAuthorize} deniega dentro del
     * DispatcherServlet (via AOP sobre el metodo del controller), asi que la excepcion llega
     * hasta este handler antes de poder alcanzar el {@code AccessDeniedHandler} de Spring
     * Security a nivel de filtro. Es el unico punto por el que pasan todas.
     *
     * <p>La auditoria va en su propia transaccion (la de {@code AuditService}), asi que no la
     * arrastra el rollback del request fallido. El metodo y la ruta pedidos van en la metadata:
     * sin eso, "acceso denegado" no dice a que.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        auditarIntento(request);

        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.SIN_PERMISO.getStatus().value(),
                ApiErrorCode.SIN_PERMISO.name(),
                "No tenés permiso para realizar esta acción.",
                null,
                request.getRequestURI()
        );
        return ResponseEntity.status(ApiErrorCode.SIN_PERMISO.getStatus()).body(body);
    }

    /**
     * El actor sale del contexto de seguridad, no del request: en un 403 hay sesion valida (el
     * token se verifico), lo que falta es el permiso. Si no hubiera sesion el rechazo seria 401
     * y ni siquiera llegaria hasta aca.
     */
    private void auditarIntento(HttpServletRequest request) {
        UUID actorId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID id) {
            actorId = id;
        }
        AuditService servicio = auditService.getIfAvailable();
        if (servicio == null) {
            return;
        }
        try {
            servicio.registrar(
                    actorId, AuditAccion.ACCESO_DENEGADO, "Acceso", null,
                    request.getMethod() + " " + request.getRequestURI());
        } catch (RuntimeException e) {
            // Auditar no puede convertir un 403 en un 500: si la escritura falla, se loguea y
            // el cliente igual recibe su respuesta.
            log.warn("No se pudo auditar un acceso denegado a {}", request.getRequestURI(), e);
        }
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        // Error del cliente (verbo HTTP equivocado para la ruta), no una falla del
        // servidor: no lo logueamos como ERROR ni lo mandamos por el catch-all a 500.
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METODO_NO_PERMITIDO",
                "El método %s no está permitido para esta ruta.".formatted(ex.getMethod()),
                null,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleParametroInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // Error del cliente (ej. un UUID mal formado en un query param), no una falla
        // del servidor.
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.VALIDACION.getStatus().value(),
                ApiErrorCode.VALIDACION.name(),
                "El parámetro '%s' tiene un valor inválido.".formatted(ex.getName()),
                Map.of(ex.getName(), "Valor inválido."),
                request.getRequestURI()
        );
        return ResponseEntity.status(ApiErrorCode.VALIDACION.getStatus()).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleArchivoDemasiadoGrande(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        // Error del cliente (archivo mas grande que spring.servlet.multipart.max-file-size),
        // no una falla del servidor.
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.VALIDACION.getStatus().value(),
                ApiErrorCode.VALIDACION.name(),
                "El archivo supera el tamaño máximo permitido (5 MB).",
                null,
                request.getRequestURI()
        );
        return ResponseEntity.status(ApiErrorCode.VALIDACION.getStatus()).body(body);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleParteFaltante(Exception ex, HttpServletRequest request) {
        // Cliente que manda un multipart incompleto (ej. el alta de instructor sin la parte
        // "documentos") o sin un query param obligatorio. Sin este handler caia en el
        // catch-all y devolvia 500, como si el problema fuera del servidor.
        String nombre = ex instanceof MissingServletRequestPartException parte
                ? parte.getRequestPartName()
                : ((MissingServletRequestParameterException) ex).getParameterName();
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.VALIDACION.getStatus().value(),
                ApiErrorCode.VALIDACION.name(),
                "Falta el campo obligatorio '%s' en el pedido.".formatted(nombre),
                Map.of(nombre, "Es obligatorio."),
                request.getRequestURI()
        );
        return ResponseEntity.status(ApiErrorCode.VALIDACION.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.VALIDACION.getStatus().value(),
                ApiErrorCode.VALIDACION.name(),
                "Hay errores de validación en los datos enviados.",
                fieldErrors,
                request.getRequestURI()
        );
        return ResponseEntity.status(ApiErrorCode.VALIDACION.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(Exception ex, HttpServletRequest request) {
        log.error("Error interno no controlado en {}", request.getRequestURI(), ex);
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ApiErrorCode.ERROR_INTERNO.name(),
                "Ocurrió un error inesperado. Intentá de nuevo más tarde.",
                null,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
