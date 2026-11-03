package com.activehub.usecases.listarmissanciones;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.shared.security.PenalizacionVigenteGuard;
import com.activehub.shared.time.Zonas;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las sanciones del propio usuario (V36). Antes no habia ninguna pantalla donde el
 * penalizado viera la suya: el aviso llegaba sin link y el ABM es del administrador.
 *
 * <p>Sin permiso propio, como "Mis datos" del instructor: es la cuenta del que llama, no un
 * modulo. Tiene que responder tambien con la multa sin pagar, porque es justo la pantalla
 * donde la paga.
 */
@Service
public class ListarMisSancionesService {

    private final PenalizacionRepository penalizacionRepository;
    private final ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    private final PenalizacionVigenteGuard penalizacionVigenteGuard;
    private final Clock clock;
    private final ListarMisSancionesResponse.Transferencia transferencia;

    public ListarMisSancionesService(
            PenalizacionRepository penalizacionRepository,
            ComprobantePenalizacionRepository comprobantePenalizacionRepository,
            PenalizacionVigenteGuard penalizacionVigenteGuard,
            Clock clock,
            @Value("${app.penalizaciones.transferencia.alias:}") String alias,
            @Value("${app.penalizaciones.transferencia.cbu:}") String cbu,
            @Value("${app.penalizaciones.transferencia.titular:}") String titular) {
        this.penalizacionRepository = penalizacionRepository;
        this.comprobantePenalizacionRepository = comprobantePenalizacionRepository;
        this.penalizacionVigenteGuard = penalizacionVigenteGuard;
        this.clock = clock;
        this.transferencia = new ListarMisSancionesResponse.Transferencia(
                vacioANull(alias), vacioANull(cbu), vacioANull(titular));
    }

    @Transactional(readOnly = true)
    public ListarMisSancionesResponse listar(UUID usuarioId) {
        LocalDate hoy = LocalDate.now(clock.withZone(Zonas.AR));
        List<Penalizacion> sanciones = penalizacionRepository.findDeUsuario(usuarioId);

        List<UUID> multas = sanciones.stream()
                .filter(p -> p.getTipo() == TipoPenalizacion.ECONOMICA)
                .map(Penalizacion::getId)
                .toList();
        // Con la lista vacia el IN de la consulta no es valido en Postgres.
        Map<UUID, List<ComprobantePenalizacion>> comprobantes = multas.isEmpty()
                ? Map.of()
                : comprobantePenalizacionRepository.findDePenalizaciones(multas).stream()
                        .collect(Collectors.groupingBy(c -> c.getPenalizacion().getId()));

        return new ListarMisSancionesResponse(
                penalizacionVigenteGuard.tieneMultaPendiente(usuarioId),
                penalizacionVigenteGuard.suspensionVigente(usuarioId).map(Penalizacion::getFechaFin).orElse(null),
                transferencia,
                sanciones.stream()
                        .map(p -> new ListarMisSancionesResponse.Sancion(
                                p.getId(),
                                p.getTipo().getEtiqueta(),
                                p.getMotivo(),
                                p.getMonto(),
                                p.getFechaInicio(),
                                p.getFechaFin(),
                                p.getCreatedAt(),
                                p.getEstadoPago() != null ? p.getEstadoPago().name() : null,
                                rigeHoy(p, hoy),
                                p.getLevantadaAt(),
                                p.getMotivoLevantamiento(),
                                comprobantes.getOrDefault(p.getId(), List.of()).stream()
                                        .map(c -> new ListarMisSancionesResponse.Comprobante(
                                                c.getId(), c.getNombreArchivo(), c.getEstado().name(),
                                                c.getMotivoInvalidez(), c.getCreatedAt()))
                                        .toList()))
                        .toList());
    }

    private static boolean rigeHoy(Penalizacion p, LocalDate hoy) {
        return p.getTipo() == TipoPenalizacion.SUSPENSION_TEMPORAL
                && p.getLevantadaAt() == null
                && p.getFechaInicio() != null && p.getFechaFin() != null
                && !hoy.isBefore(p.getFechaInicio()) && !hoy.isAfter(p.getFechaFin());
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
