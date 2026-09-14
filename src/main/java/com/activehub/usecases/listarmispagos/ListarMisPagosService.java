package com.activehub.usecases.listarmispagos;

import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E3A-HU07: historial de pagos del alumno logueado.
 *
 * <p>No existia ningun endpoint de pagos del alumno, asi que la pantalla "Mis pagos" no tenia
 * de donde sacar el estado real (Retenido / Liberado / Cancelado / Efectivo) — que es
 * justamente el objetivo de la HU.
 *
 * <p>Se arma sobre las inscripciones del alumno porque el Pago cuelga de la Inscripcion: se
 * listan las que tienen pago asociado, mas nuevas primero.
 */
@Service
public class ListarMisPagosService {

    private final InscripcionRepository inscripcionRepository;

    public ListarMisPagosService(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarMisPagosResponse> listar(UUID alumnoId) {
        return inscripcionRepository.findByAlumnoIdConDetalle(alumnoId, null).stream()
                .filter(i -> i.getPago() != null)
                .map(this::aRespuesta)
                .toList();
    }

    private ListarMisPagosResponse aRespuesta(Inscripcion i) {
        var pago = i.getPago();
        var clase = i.getClase();
        return new ListarMisPagosResponse(
                pago.getId(),
                i.getId(),
                clase.getId(),
                clase.getActividad().getId(),
                clase.getActividad().getNombre(),
                clase.getFechaHora(),
                clase.getEstado().name(),
                i.getEstado().getEtiqueta(),
                pago.getEstado().name(),
                pago.getMetodo().getEtiqueta(),
                pago.getMonto(),
                pago.getCreatedAt());
    }
}
