package com.activehub.usecases.listarpenalizaciones;

import com.activehub.domain.penalizacion.ComprobantePenalizacion;
import com.activehub.domain.penalizacion.ComprobantePenalizacionRepository;
import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado de penalizaciones para el admin.
 *
 * <p>Antes no existia ningun GET de penalizaciones: la pantalla leia el dataset mock del
 * frontend, con dos consecuencias opuestas y ambas malas — lo que el admin "aplicaba" no se
 * guardaba, y las Penalizacion que si se persistian al resolver una denuncia no aparecian
 * nunca en la pantalla.
 *
 * <p>V36: cada multa trae el estado de su pago y sus comprobantes, y cada suspension si se
 * levanto antes de tiempo. Es de donde el admin revisa el comprobante y desbloquea.
 */
@Service
public class ListarPenalizacionesService {

    private static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

    private final PenalizacionRepository penalizacionRepository;
    private final ComprobantePenalizacionRepository comprobantePenalizacionRepository;
    private final Clock clock;

    public ListarPenalizacionesService(
            PenalizacionRepository penalizacionRepository,
            ComprobantePenalizacionRepository comprobantePenalizacionRepository,
            Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.comprobantePenalizacionRepository = comprobantePenalizacionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ListarPenalizacionesResponse> listar() {
        LocalDate hoy = LocalDate.ofInstant(clock.instant(), ZONA_AR);
        List<Penalizacion> penalizaciones = penalizacionRepository.findAllConDetalle();

        List<UUID> multas = penalizaciones.stream()
                .filter(p -> p.getTipo() == TipoPenalizacion.ECONOMICA)
                .map(Penalizacion::getId)
                .toList();
        // Con la lista vacia el IN de la consulta no es valido en Postgres.
        Map<UUID, List<ComprobantePenalizacion>> comprobantes = multas.isEmpty()
                ? Map.of()
                : comprobantePenalizacionRepository.findDePenalizaciones(multas).stream()
                        .collect(Collectors.groupingBy(c -> c.getPenalizacion().getId()));

        return penalizaciones.stream()
                .map(p -> new ListarPenalizacionesResponse(
                        p.getId(),
                        p.getUsuario().getId(),
                        p.getUsuario().getNombre() + " " + p.getUsuario().getApellido(),
                        p.getUsuario().getEmail(),
                        p.getTipo().getEtiqueta(),
                        p.getMotivo(),
                        p.getMonto(),
                        p.getFechaInicio(),
                        p.getFechaFin(),
                        esVigente(p, hoy),
                        p.getDenuncia() != null ? p.getDenuncia().getId() : null,
                        p.getUsuario().getCantidadPenalizaciones(),
                        p.getCreatedAt(),
                        p.getEstadoPago() != null ? p.getEstadoPago().name() : null,
                        p.getPagoAprobadoAt(),
                        p.getLevantadaAt(),
                        p.getMotivoLevantamiento(),
                        comprobantes.getOrDefault(p.getId(), List.of()).stream()
                                .map(c -> new ListarPenalizacionesResponse.Comprobante(
                                        c.getId(), c.getNombreArchivo(), c.getEstado().name(),
                                        c.getMotivoInvalidez(), c.getCreatedAt(), c.getRevisadoAt()))
                                .toList()))
                .toList();
    }

    /**
     * Solo las suspensiones tienen vigencia; una economica se aplica y se acabo. Una suspension
     * levantada por un administrador (V36) ya no esta vigente.
     */
    private boolean esVigente(Penalizacion p, LocalDate hoy) {
        if (p.getTipo() != TipoPenalizacion.SUSPENSION_TEMPORAL || p.getFechaFin() == null
                || p.getLevantadaAt() != null) {
            return false;
        }
        boolean yaEmpezo = p.getFechaInicio() == null || !hoy.isBefore(p.getFechaInicio());
        return yaEmpezo && !hoy.isAfter(p.getFechaFin());
    }
}
