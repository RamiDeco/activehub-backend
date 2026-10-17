package com.activehub.usecases.listarrosterclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.math.BigDecimal;
import java.util.List;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarRosterClaseService {

    private static final List<EstadoInscripcion> ESTADOS_ROSTER =
            List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.PAGO_PENDIENTE);

    /**
     * Una clase cancelada ya no tiene a nadie Inscripto: la cascada de la cancelacion paso
     * todas sus inscripciones a Cancelada. Desde el Historial el instructor quiere ver quienes
     * estaban anotados y que paso con su pago (reintegro), asi que en ese caso se listan
     * tambien las canceladas. En una clase viva no: el roster es la lista de quien viene.
     */
    private static final List<EstadoInscripcion> ESTADOS_ROSTER_CANCELADA =
            List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.PAGO_PENDIENTE, EstadoInscripcion.CANCELADA);

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;

    private final InstructorVerificadoGuard instructorVerificadoGuard;

    public ListarRosterClaseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            InstructorVerificadoGuard instructorVerificadoGuard
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
    }

    @Transactional(readOnly = true)
    public ListarRosterClaseResponse listar(UUID claseId, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "ver el listado de alumnos");
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés ver el roster de una clase que no te pertenece.");
        }

        var inscripciones = inscripcionRepository.findByClaseIdAndEstadoInOrderByCreatedAtAsc(
                claseId, clase.getEstado() == EstadoClase.Cancelada ? ESTADOS_ROSTER_CANCELADA : ESTADOS_ROSTER);

        var alumnos = inscripciones.stream().map(ListarRosterClaseService::aAlumno).toList();

        int cantidadInscripto = (int) inscripciones.stream().filter(i -> i.getEstado() == EstadoInscripcion.INSCRIPTO).count();
        int cantidadPagoPendiente = (int) inscripciones.stream().filter(i -> i.getEstado() == EstadoInscripcion.PAGO_PENDIENTE).count();
        int cantidadPreInscripcion = (int) inscripcionRepository.countByClaseIdAndEstado(claseId, EstadoInscripcion.PRE_INSCRIPCION);

        return new ListarRosterClaseResponse(
                clase.getId(),
                clase.getPrecio(),
                clase.getCuposMax(),
                clase.getCuposOcupados(),
                clase.getCuposMax() - clase.getCuposOcupados(),
                cantidadInscripto,
                cantidadPagoPendiente,
                cantidadPreInscripcion,
                alumnos
        );
    }

    private static ListarRosterClaseResponse.Alumno aAlumno(Inscripcion i) {
        Pago pago = i.getPago();
        return new ListarRosterClaseResponse.Alumno(
                i.getId(),
                i.getAlumno().getId(),
                i.getAlumno().getNombre(),
                i.getAlumno().getApellido(),
                i.getAlumno().getTelefono(),
                i.getEstado().getEtiqueta(),
                pago != null ? pago.getEstado().name() : null,
                pago != null ? pago.getMetodo().getEtiqueta() : null,
                pago != null && pago.getCapturadoAt() != null,
                pago != null ? neto(pago) : null);
    }

    /** Lo que recibe el instructor: el monto ya calculado si lo hay, si no monto menos comision. */
    private static BigDecimal neto(Pago pago) {
        if (pago.getMontoInstructor() != null) {
            return pago.getMontoInstructor();
        }
        BigDecimal comision = pago.getComision() != null ? pago.getComision() : BigDecimal.ZERO;
        return pago.getMonto().subtract(comision);
    }
}
