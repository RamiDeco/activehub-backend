package com.activehub.usecases.crearreportesoporte;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint <b>publico</b> (ver la lista {@code permitAll} de {@code SecurityConfig}): la
 * pantalla de Ayuda es publica y quien necesita soporte muchas veces es justamente alguien que
 * no pudo entrar. Por eso no lleva {@code @PreAuthorize} y {@code authentication} puede venir
 * en null; el service lo contempla.
 */
@RestController
@RequestMapping("/api/soporte/reportes")
public class CrearReporteSoporteController {

    private final CrearReporteSoporteService crearReporteSoporteService;

    public CrearReporteSoporteController(CrearReporteSoporteService crearReporteSoporteService) {
        this.crearReporteSoporteService = crearReporteSoporteService;
    }

    @PostMapping
    public ResponseEntity<CrearReporteSoporteResponse> crear(
            @Valid @RequestBody CrearReporteSoporteRequest request, Authentication authentication
    ) {
        UUID autorId = authentication == null ? null : (UUID) authentication.getPrincipal();
        CrearReporteSoporteResponse response = crearReporteSoporteService.crear(request, autorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
