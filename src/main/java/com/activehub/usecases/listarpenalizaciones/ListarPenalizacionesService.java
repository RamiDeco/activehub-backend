package com.activehub.usecases.listarpenalizaciones;

import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado de penalizaciones para el admin.
 *
 * <p>Antes no existia ningun GET de penalizaciones: la pantalla leia el dataset mock del
 * frontend, con dos consecuencias opuestas y ambas malas — lo que el admin "aplicaba" no se
 * guardaba, y las Penalizacion que si se persistian al resolver una denuncia no aparecian
 * nunca en la pantalla.
 */
@Service
public class ListarPenalizacionesService {

    private static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

    private final PenalizacionRepository penalizacionRepository;
    private final Clock clock;

    public ListarPenalizacionesService(PenalizacionRepository penalizacionRepository, Clock clock) {
        this.penalizacionRepository = penalizacionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ListarPenalizacionesResponse> listar() {
        LocalDate hoy = LocalDate.ofInstant(clock.instant(), ZONA_AR);

        return penalizacionRepository.findAllConDetalle().stream()
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
                        p.getCreatedAt()))
                .toList();
    }

    /** Solo las suspensiones tienen vigencia; una economica se aplica y se acabo. */
    private boolean esVigente(Penalizacion p, LocalDate hoy) {
        if (p.getTipo() != TipoPenalizacion.SUSPENSION_TEMPORAL || p.getFechaFin() == null) {
            return false;
        }
        boolean yaEmpezo = p.getFechaInicio() == null || !hoy.isBefore(p.getFechaInicio());
        return yaEmpezo && !hoy.isAfter(p.getFechaFin());
    }
}
