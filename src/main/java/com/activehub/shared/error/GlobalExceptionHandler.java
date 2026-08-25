package com.activehub.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // @PreAuthorize deniega dentro del DispatcherServlet (via AOP sobre el metodo del
        // controller), asi que la excepcion llega hasta aca antes de poder alcanzar el
        // AccessDeniedHandler de Spring Security a nivel de filtro.
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
