package com.activehub.usecases.eliminarclase;

import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.ClaseConInscriptosException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.security.InstructorVerificadoGuard;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarClaseService {

    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AuditService auditService;
    private final InstructorVerificadoGuard instructorVerificadoGuard;

    public EliminarClaseService(
            ClaseRepository claseRepository,
            InscripcionRepository inscripcionRepository,
            AuditService auditService,
            InstructorVerificadoGuard instructorVerificadoGuard
    ) {
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.auditService = auditService;
        this.instructorVerificadoGuard = instructorVerificadoGuard;
    }

    @Transactional
    public void eliminar(UUID id, UUID instructorId) {
        instructorVerificadoGuard.exigirVerificado(instructorId, "eliminar clases");

        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("Clase no encontrada."));

        if (!clase.getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés eliminar una clase que no te pertenece.");
        }

        // E2I-HU05 criterio 7: solo se elimina una clase SIN inscriptos. Este borrado es
        // una baja logica pelada (no cancela inscripciones, no reintegra y no notifica),
        // asi que dejarlo pasar con alumnos anotados les hacia perder el registro de una
        // clase que pagaron: la Clase queda invisible por @SQLRestriction y la inscripcion
        // desaparece de su historial sin haber pasado nunca a Cancelada.
        // Para una clase con gente anotada el camino correcto es cancelarla (cancelarclase),
        // que si hace la cascada completa.
        if (inscripcionRepository.existsByClaseIdAndEstadoNot(id, EstadoInscripcion.CANCELADA)) {
            throw new ClaseConInscriptosException();
        }

        clase.marcarBorrado();
        claseRepository.save(clase);

        auditService.registrar(instructorId, AuditAccion.CLASE_ELIMINADA, "Clase", id, null);
    }
}
