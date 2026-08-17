package com.activehub.usecases.listarrosterclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarRosterClaseService {

    private static final List<EstadoInscripcion> ESTADOS_ROSTER =
            List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.PAGO_PENDIENTE);

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;

    public ListarRosterClaseService(ClaseRepository claseRepository, InscripcionRepository inscripcionRepository) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public ListarRosterClaseResponse listar(UUID claseId, UUID instructorId) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés ver el roster de una clase que no te pertenece.");
        }

        var inscripciones = inscripcionRepository.findByClaseIdAndEstadoInOrderByCreatedAtAsc(claseId, ESTADOS_ROSTER);

        var alumnos = inscripciones.stream()
                .map(i -> new ListarRosterClaseResponse.Alumno(
                        i.getId(),
                        i.getAlumno().getId(),
                        i.getAlumno().getNombre(),
                        i.getAlumno().getApellido(),
                        i.getAlumno().getTelefono(),
                        i.getEstado().getEtiqueta(),
                        i.getPresente()))
                .toList();

        int cantidadInscripto = (int) inscripciones.stream().filter(i -> i.getEstado() == EstadoInscripcion.INSCRIPTO).count();
        int cantidadPagoPendiente = (int) inscripciones.stream().filter(i -> i.getEstado() == EstadoInscripcion.PAGO_PENDIENTE).count();
        int cantidadPreInscripcion = (int) inscripcionRepository.countByClaseIdAndEstado(claseId, EstadoInscripcion.PRE_INSCRIPCION);

        return new ListarRosterClaseResponse(
                clase.getId(),
                clase.getCuposMax(),
                clase.getCuposOcupados(),
                clase.getCuposMax() - clase.getCuposOcupados(),
                cantidadInscripto,
                cantidadPagoPendiente,
                cantidadPreInscripcion,
                alumnos
        );
    }
}
