package com.activehub.usecases.marcarasistencia;

import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.SinPermisoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarcarAsistenciaService {

    private final InscripcionRepository inscripcionRepository;
    private final AuditService auditService;

    public MarcarAsistenciaService(InscripcionRepository inscripcionRepository, AuditService auditService) {
        this.inscripcionRepository = inscripcionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MarcarAsistenciaResponse marcar(UUID inscripcionId, MarcarAsistenciaRequest request, UUID instructorId) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new NoEncontradoException("Inscripción no encontrada."));

        if (!inscripcion.getClase().getActividad().getInstructor().getId().equals(instructorId)) {
            throw new SinPermisoException("No podés marcar asistencia de una clase que no te pertenece.");
        }

        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            throw new ValidacionException("No podés marcar asistencia de una inscripción cancelada.");
        }

        inscripcion.setPresente(request.presente());
        inscripcionRepository.save(inscripcion);

        auditService.registrar(instructorId, AuditAccion.ASISTENCIA_MARCADA, "Inscripcion", inscripcion.getId(), null);

        return new MarcarAsistenciaResponse(inscripcion.getId(), inscripcion.getPresente());
    }
}
